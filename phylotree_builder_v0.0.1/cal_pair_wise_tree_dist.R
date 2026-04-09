# 加载必要的R包
library(ape)
library(TreeDist)

# 读取TSV文件并提取树文件路径
read_tree_metadata <- function(tsv_file) {
  # 读取TSV文件
  metadata <- read.table(tsv_file, sep = "\t", header = FALSE, 
                        col.names = c("Method", "TreeFile"), 
                        stringsAsFactors = FALSE)
  return(metadata)
}

# 读取所有进化树
read_all_trees <- function(metadata) {
  trees <- list()
  tree_names <- c()
  
  for (i in 1:nrow(metadata)) {
    method <- metadata$Method[i]
    tree_file <- metadata$TreeFile[i]
    
    # 检查文件是否存在
    if (file.exists(tree_file)) {
      # 读取树文件
      tree <- read.tree(tree_file)
      trees[[method]] <- tree
      tree_names <- c(tree_names, method)
      cat("成功读取树:", method, "\n")
    } else {
      cat("警告: 文件不存在:", tree_file, "\n")
    }
  }
  
  return(list(trees = trees, names = tree_names))
}

# 计算距离矩阵
calculate_distance_matrices <- function(trees, tree_names) {
  n_trees <- length(trees)
  
  # 初始化距离矩阵
  tree_distance_matrix <- matrix(0, nrow = n_trees, ncol = n_trees)
  rf_distance_matrix <- matrix(0, nrow = n_trees, ncol = n_trees)
  
  # 设置行名和列名
  rownames(tree_distance_matrix) <- tree_names
  colnames(tree_distance_matrix) <- tree_names
  rownames(rf_distance_matrix) <- tree_names
  colnames(rf_distance_matrix) <- tree_names
  
  # 计算所有树对之间的距离
  for (i in 1:n_trees) {
    for (j in 1:n_trees) {
      if (i != j) {
        tree1 <- trees[[i]]
        tree2 <- trees[[j]]
        
        # 计算TreeDistance
        tree_dist <- TreeDistance(tree1, tree2)
        tree_distance_matrix[i, j] <- tree_dist
        
        # 计算Robinson-Foulds距离
        rf_dist <- RobinsonFoulds(tree1, tree2)
        rf_distance_matrix[i, j] <- rf_dist
        
        cat("计算完成:", tree_names[i], "vs", tree_names[j], 
            "TreeDist:", tree_dist, "RF:", rf_dist, "\n")
      }
    }
  }
  
  return(list(tree_distance = tree_distance_matrix, 
              rf_distance = rf_distance_matrix))
}

# 主函数
main <- function(tsv_file = "phylo_results/tree_meta_data.tsv") {
  # 读取元数据
  cat("读取树文件元数据...\n")
  metadata <- read_tree_metadata(tsv_file)
  print(metadata)
  
  # 读取所有树
  cat("\n读取进化树文件...\n")
  tree_data <- read_all_trees(metadata)
  trees <- tree_data$trees
  tree_names <- tree_data$names
  
  if (length(trees) < 2) {
    stop("至少需要2棵树才能计算距离矩阵")
  }
  
  # 计算距离矩阵
  cat("\n计算距离矩阵...\n")
  distance_matrices <- calculate_distance_matrices(trees, tree_names)
  
  # 输出结果
  cat("\n=== TreeDistance 距离矩阵 ===\n")
  print(distance_matrices$tree_distance)
  
  cat("\n=== Robinson-Foulds 距离矩阵 ===\n")
  print(distance_matrices$rf_distance)
  
  # 保存结果到文件
  write.table(distance_matrices$tree_distance, 
              "tree_distance_matrix.tsv",
              sep = "\t", row.names = TRUE, col.names = TRUE, quote = FALSE)
  write.table(distance_matrices$rf_distance, 
              "rf_distance_matrix.tsv",
              sep = "\t", row.names = TRUE, col.names = TRUE, quote = FALSE)
  
  cat("\n距离矩阵已保存到:\n")
  cat("- phylo_results/tree_distance_matrix.tsv\n")
  cat("- phylo_results/rf_distance_matrix.tsv\n")
  
  return(distance_matrices)
}

# 命令行参数处理
args <- commandArgs(trailingOnly = TRUE)

# 检查命令行参数
if (length(args) != 1) {
  cat("用法: Rscript cal_pair_wise_tree_dist.R <tree_meta_data.tsv>\n")
  cat("示例: Rscript cal_pair_wise_tree_dist.R phylo_results/tree_meta_data.tsv\n")
  quit(status = 1)
}

# 获取输入文件路径
tsv_file <- args[1]

# 检查输入文件是否存在
if (!file.exists(tsv_file)) {
  cat("错误: 输入文件不存在:", tsv_file, "\n")
  quit(status = 1)
}

# 运行主函数
cat("开始处理文件:", tsv_file, "\n")
result <- main(tsv_file)
cat("处理完成!\n")