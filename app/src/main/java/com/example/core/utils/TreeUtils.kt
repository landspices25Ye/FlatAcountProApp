package com.example.core.utils

interface ITreeNode {
    val id: Long
    val parentId: Long?
}

object TreeUtils {

    class TreeNode<T : ITreeNode>(
        val data: T,
        val children: MutableList<TreeNode<T>> = mutableListOf()
    )

    /**
     * Build standard hierarchical trees from a flat list
     */
    fun <T : ITreeNode> buildTree(flatList: List<T>): List<TreeNode<T>> {
        val nodeMap = flatList.associateBy { it.id }
        val roots = mutableListOf<TreeNode<T>>()
        val treeNodes = flatList.associate { it.id to TreeNode(it) }

        for (item in flatList) {
            val currentNode = treeNodes[item.id] ?: continue
            if (item.parentId == null || !nodeMap.containsKey(item.parentId)) {
                roots.add(currentNode)
            } else {
                val parentNode = treeNodes[item.parentId]
                parentNode?.children?.add(currentNode)
            }
        }
        
        // Sorting children/roots by code if available in any manner or using identity
        return roots
    }

    /**
     * Flatten a tree down to flat structures with accurate depth estimation
     */
    fun <T : ITreeNode> flattenTreeWithDepth(roots: List<TreeNode<T>>, currentDepth: Int = 0): List<Pair<T, Int>> {
        val result = mutableListOf<Pair<T, Int>>()
        for (root in roots) {
            result.add(Pair(root.data, currentDepth))
            result.addAll(flattenTreeWithDepth(root.children, currentDepth + 1))
        }
        return result
    }

    /**
     * Get all descendants IDs for a node
     */
    fun <T : ITreeNode> getDescendantIds(nodeId: Long, flatList: List<T>): List<Long> {
        val tree = buildTree(flatList)
        val targetNode = findNodeInTree(nodeId, tree) ?: return emptyList()
        val ids = mutableListOf<Long>()
        collectDescendantIds(targetNode, ids)
        return ids
    }

    private fun <T : ITreeNode> findNodeInTree(id: Long, nodes: List<TreeNode<T>>): TreeNode<T>? {
        for (n in nodes) {
            if (n.data.id == id) return n
            val childResult = findNodeInTree(id, n.children)
            if (childResult != null) return childResult
        }
        return null
    }

    private fun <T : ITreeNode> collectDescendantIds(node: TreeNode<T>, out: MutableList<Long>) {
        for (child in node.children) {
            out.add(child.data.id)
            collectDescendantIds(child, out)
        }
    }
}
