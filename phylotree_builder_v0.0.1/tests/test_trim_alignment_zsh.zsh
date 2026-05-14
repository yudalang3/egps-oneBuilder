#!/bin/zsh

set -euo pipefail

script_dir="${0:a:h}"
repo_dir="${script_dir:h}"
repo_root="${repo_dir:h}"
test_root="$(mktemp -d /tmp/onebuilder-trim-tests.XXXXXX)"
runtime_dir="$test_root/runtime"
trimal_argv_file="$test_root/trimal-argv.txt"
s2_argv_file="$test_root/s2-argv.txt"
trim_called_file="$test_root/trim-called.txt"

fail() {
    print -u2 -- "FAIL $1"
    exit 1
}

pass() {
    print -- "PASS $1"
}

skip() {
    print -- "SKIP $1"
}

assert_file_exists() {
    [[ -f "$1" ]] || fail "expected file to exist: $1"
}

assert_file_not_exists() {
    [[ ! -e "$1" ]] || fail "expected file not to exist: $1"
}

assert_contains() {
    local file="$1"
    local expected="$2"
    grep -F -- "$expected" "$file" >/dev/null || fail "expected '$file' to contain: $expected"
}

assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="$3"
    [[ "$expected" == "$actual" ]] || fail "$message expected=[$expected] actual=[$actual]"
}

expect_failure() {
    local label="$1"
    local expected="$2"
    shift 2
    local output_file="$test_root/${label}.out"
    if "$@" >"$output_file" 2>&1; then
        fail "$label should have failed"
    fi
    assert_contains "$output_file" "$expected"
    pass "$label"
}

cleanup() {
    chmod -R u+rwX "$test_root" 2>/dev/null || true
    rm -rf "$test_root"
}
trap cleanup EXIT

setup_runtime() {
    mkdir -p "$runtime_dir/bin"
    local python_exe
    if command -v python3 >/dev/null 2>&1; then
        python_exe="$(command -v python3)"
    elif command -v python >/dev/null 2>&1; then
        python_exe="$(command -v python)"
    else
        fail "python3 or python is required for JSON-backed wrapper tests"
    fi
    {
        print -- "#!/bin/sh"
        print -- "exec \"$python_exe\" \"\$@\""
    } >"$runtime_dir/bin/python3.13"
    chmod +x "$runtime_dir/bin/python3.13"
}

write_fasta() {
    local output_file="$1"
    {
        print -- ">seq1"
        print -- "ACGTACGT"
        print -- ">seq2"
        print -- "ACG-ACGT"
        print -- ">seq3"
        print -- "ACGTAC-T"
    } >"$output_file"
}

json_array() {
    local first=1
    print -n -- "["
    for value in "$@"; do
        if [[ "$first" -eq 0 ]]; then
            print -n -- ", "
        fi
        first=0
        print -n -- "\"$value\""
    done
    print -n -- "]"
}

write_config() {
    local config_file="$1"
    local input_file="$2"
    local output_dir="$3"
    local output_prefix="$4"
    local input_type="$5"
    local run_alignment_first="$6"
    local trim_enabled="$7"
    local trim_args_json="$8"
    {
        print -- "{"
        print -- "  \"run\": {"
        print -- "    \"input_type\": \"$input_type\","
        print -- "    \"input_file\": \"$input_file\","
        print -- "    \"output_base_dir\": \"$output_dir\","
        print -- "    \"output_prefix\": \"$output_prefix\""
        print -- "  },"
        print -- "  \"alignment\": {"
        print -- "    \"run_alignment_first\": $run_alignment_first"
        print -- "  },"
        print -- "  \"trim_alignment\": {"
        print -- "    \"enabled\": $trim_enabled,"
        print -- "    \"trimal\": {"
        print -- "      \"common\": {"
        print -- "        \"args\": $trim_args_json"
        print -- "      }"
        print -- "    }"
        print -- "  }"
        print -- "}"
    } >"$config_file"
}

install_trimal_stub() {
    local stub="$test_root/trimal-stub.zsh"
    cat >"$stub" <<'EOF'
#!/bin/zsh
set -euo pipefail
: >"$TRIMAL_ARGV_FILE"
for arg in "$@"; do
    print -r -- "$arg" >>"$TRIMAL_ARGV_FILE"
done
local input_file=""
local output_file=""
local args=("$@")
for ((index = 1; index <= ${#args[@]}; index++)); do
    case "${args[$index]}" in
        -in)
            input_file="${args[$((index + 1))]:-}"
            ;;
        -out)
            output_file="${args[$((index + 1))]:-}"
            ;;
    esac
done
[[ -n "$input_file" ]] || { print -u2 -- "stub missing -in"; exit 2; }
[[ -n "$output_file" ]] || { print -u2 -- "stub missing -out"; exit 2; }
cp "$input_file" "$output_file"
print -- "stub trimAl wrote $output_file"
EOF
    chmod +x "$stub"
    print -- "$stub"
}

read_file() {
    local file="$1"
    if [[ -f "$file" ]]; then
        <"$file"
    fi
}

run_real_trimal_smoke() {
    local real_trimal="/opt/bioinfor/trimAI/bin/trimal"
    if [[ ! -x "$real_trimal" ]]; then
        skip "real trimAl smoke: /opt/bioinfor/trimAI/bin/trimal is absent"
        return
    fi
    local work_dir="$test_root/real-smoke"
    mkdir -p "$work_dir"
    local input_file="$work_dir/input.fasta"
    local config_file="$work_dir/config.json"
    write_fasta "$input_file"
    write_config "$config_file" "$input_file" "$work_dir/out" "trim_real" "DNA_CDS" "false" "true" "$(json_array -gt 0.9 -cons 60)"

    ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file" >/dev/null
    assert_file_exists "$work_dir/input.trim.fasta"
    assert_file_exists "$work_dir/input.trim.fasta.trimal.log"
    pass "real trimAl smoke"
}

run_trimal_argument_tests() {
    local stub
    stub="$(install_trimal_stub)"
    export TRIMAL_EXE="$stub"
    export TRIMAL_ARGV_FILE="$trimal_argv_file"

    local work_dir="$test_root/args"
    mkdir -p "$work_dir"
    local input_file="$work_dir/input.fasta"
    local config_file="$work_dir/config.json"
    write_fasta "$input_file"
    write_config "$config_file" "$input_file" "$work_dir/out" "trim_args" "DNA_CDS" "false" "true" "$(json_array -gt 0.9 -cons 60)"

    ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file" >/dev/null
    assert_file_exists "$work_dir/input.trim.fasta"
    assert_equals $'-in\n'"$input_file"$'\n-out\n'"$work_dir/input.trim.fasta"$'\n-gt\n0.9\n-cons\n60' "$(read_file "$trimal_argv_file")" "preset trimAl argv mismatch"

    write_config "$config_file" "$input_file" "$work_dir/out" "trim_custom" "DNA_CDS" "false" "true" "$(json_array -gt 0.8 -st 0.001)"
    ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file" >/dev/null
    assert_equals $'-in\n'"$input_file"$'\n-out\n'"$work_dir/input.trim.fasta"$'\n-gt\n0.8\n-st\n0.001' "$(read_file "$trimal_argv_file")" "custom trimAl argv mismatch"

    pass "trimAl argument forwarding"
}

run_output_naming_tests() {
    local stub
    stub="$(install_trimal_stub)"
    export TRIMAL_EXE="$stub"
    export TRIMAL_ARGV_FILE="$trimal_argv_file"

    local work_dir="$test_root/naming"
    mkdir -p "$work_dir"
    local config_file="$work_dir/config.json"
    for pair in "a.fasta:a.trim.fasta" "a.aligned.fa:a.aligned.trim.fa" "a:a.trim"; do
        local input_name="${pair%%:*}"
        local output_name="${pair##*:}"
        local input_file="$work_dir/$input_name"
        write_fasta "$input_file"
        write_config "$config_file" "$input_file" "$work_dir/out" "trim_names" "DNA_CDS" "false" "true" "$(json_array -gt 0.9 -cons 60)"
        ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file" >/dev/null
        assert_file_exists "$work_dir/$output_name"
    done
    pass "trim output naming"
}

run_error_path_tests() {
    local stub
    stub="$(install_trimal_stub)"
    export TRIMAL_EXE="$stub"
    export TRIMAL_ARGV_FILE="$trimal_argv_file"

    local work_dir="$test_root/errors"
    mkdir -p "$work_dir"
    local input_file="$work_dir/input.fasta"
    local config_file="$work_dir/config.json"
    write_fasta "$input_file"

    expect_failure "missing-config" "config file" env ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$work_dir/missing.json" "$input_file"

    write_config "$config_file" "$input_file" "$work_dir/out" "trim_false" "DNA_CDS" "false" "false" "$(json_array -gt 0.9)"
    expect_failure "trim-disabled" "trim_alignment.enabled is false" env ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file"

    write_config "$config_file" "$input_file" "$work_dir/out" "trim_no_args" "DNA_CDS" "false" "true" "[]"
    expect_failure "missing-trim-args" "no trimAl arguments" env ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file"

    write_config "$config_file" "$input_file" "$work_dir/out" "trim_bad_in" "DNA_CDS" "false" "true" "$(json_array -in x.fasta)"
    expect_failure "reserved-in" "do not pass -in or -out" env ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file"

    write_config "$config_file" "$input_file" "$work_dir/out" "trim_bad_out" "DNA_CDS" "false" "true" "$(json_array -out x.fasta)"
    expect_failure "reserved-out" "do not pass -in or -out" env ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$input_file"

    write_config "$config_file" "$work_dir/missing.fasta" "$work_dir/out" "trim_missing_input" "DNA_CDS" "false" "true" "$(json_array -gt 0.9)"
    expect_failure "missing-input" "input file" env ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$repo_dir/s1_trim_alignment.zsh" --config "$config_file" "$work_dir/missing.fasta"
}

install_run_config_fixture() {
    local fixture_dir="$1"
    mkdir -p "$fixture_dir"
    cp "$repo_dir/run_onebuilder_config.zsh" "$fixture_dir/run_onebuilder_config.zsh"
    cp "$repo_dir/s1_trim_alignment.zsh" "$fixture_dir/s1_trim_alignment.zsh"

    cat >"$fixture_dir/s1_quick_align.zsh" <<'EOF'
#!/bin/zsh
set -euo pipefail
input_file="${@[-1]}"
if [[ "$input_file" == *.* ]]; then
    output_file="${input_file%.*}.aligned.${input_file##*.}"
else
    output_file="${input_file}.aligned"
fi
cp "$input_file" "$output_file"
print -- "alignment stub wrote $output_file"
EOF
    chmod +x "$fixture_dir/s1_quick_align.zsh"

    for build_script in s2_phylo_4dna.zsh s2_phylo_4prot.zsh; do
        cat >"$fixture_dir/$build_script" <<'EOF'
#!/bin/zsh
set -euo pipefail
: >"$S2_ARGV_FILE"
for arg in "$@"; do
    print -r -- "$arg" >>"$S2_ARGV_FILE"
done
print -- "s2 stub"
EOF
        chmod +x "$fixture_dir/$build_script"
    done
}

run_onebuilder_config_tests() {
    local stub
    stub="$(install_trimal_stub)"
    export TRIMAL_EXE="$stub"
    export TRIMAL_ARGV_FILE="$trimal_argv_file"
    export S2_ARGV_FILE="$s2_argv_file"

    local fixture_dir="$test_root/run-config-fixture"
    install_run_config_fixture "$fixture_dir"
    local work_dir="$test_root/run-config"
    mkdir -p "$work_dir"
    local input_file="$work_dir/input.fa"
    local config_file="$work_dir/config.json"
    write_fasta "$input_file"

    write_config "$config_file" "$input_file" "$work_dir/out" "no_align_trim" "DNA_CDS" "false" "true" "$(json_array -gt 0.9 -cons 60)"
    ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$fixture_dir/run_onebuilder_config.zsh" --force-overwrite --config "$config_file" >/dev/null
    assert_file_exists "$work_dir/input.trim.fa"
    assert_contains "$s2_argv_file" "$work_dir/input.trim.fa"

    write_config "$config_file" "$input_file" "$work_dir/out" "align_trim" "DNA_CDS" "true" "true" "$(json_array -gt 0.9 -cons 60)"
    ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$fixture_dir/run_onebuilder_config.zsh" --force-overwrite --config "$config_file" >/dev/null
    assert_file_exists "$work_dir/input.aligned.fa"
    assert_file_exists "$work_dir/input.aligned.trim.fa"
    assert_contains "$s2_argv_file" "$work_dir/input.aligned.trim.fa"

    cat >"$fixture_dir/s1_trim_alignment.zsh" <<'EOF'
#!/bin/zsh
set -euo pipefail
print -- "trim should not be called" >"$TRIM_CALLED_FILE"
exit 9
EOF
    chmod +x "$fixture_dir/s1_trim_alignment.zsh"
    export TRIM_CALLED_FILE="$trim_called_file"
    rm -f "$trim_called_file"
    write_config "$config_file" "$input_file" "$work_dir/out" "trim_disabled" "DNA_CDS" "false" "false" "$(json_array -gt 0.9 -cons 60)"
    ONEBUILDER_RUNTIME_ENV="$runtime_dir" zsh "$fixture_dir/run_onebuilder_config.zsh" --force-overwrite --config "$config_file" >/dev/null
    assert_file_not_exists "$trim_called_file"
    assert_contains "$s2_argv_file" "$input_file"

    pass "run_onebuilder_config trim integration"
}

run_apptainer_static_tests() {
    local def_file="$repo_root/apptainer_build/onebuilder.def"
    local build_file="$repo_root/apptainer_build/build.sh"
    assert_contains "$def_file" "/opt/bioinfor/trimAI /opt/bioinfor/trimAI"
    assert_contains "$def_file" "/opt/bioinfor/trimAI/bin"
    assert_contains "$def_file" "onebuilder-trim"
    assert_contains "$build_file" "/opt/bioinfor/trimAI/bin/trimal"
    assert_contains "$build_file" "trimAl executable not found"
    pass "apptainer trim packaging static checks"
}

run_optional_image_smoke() {
    local image_file="$repo_root/apptainer/onebuilder-0.0.1-linux64.sif"
    if [[ ! -f "$image_file" ]]; then
        skip "container smoke: image is absent"
        return
    fi
    if ! command -v apptainer >/dev/null 2>&1; then
        skip "container smoke: apptainer is absent"
        return
    fi
    if apptainer exec "$image_file" onebuilder-trim --help >/dev/null 2>&1; then
        pass "container onebuilder-trim help smoke"
    else
        skip "container smoke: apptainer execution is unavailable in this environment"
    fi
}

setup_runtime
run_real_trimal_smoke
run_trimal_argument_tests
run_output_naming_tests
run_error_path_tests
run_onebuilder_config_tests
run_apptainer_static_tests
run_optional_image_smoke
