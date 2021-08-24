# Homework 2

- Mengjie Ye 
- 2001212409

## BlockChain.java

### BlockNode

```java
private class BlockNode{
    public Block block;
    public int height;
    public BlockNode parentNode;
    public UTXOPool utxoPool;
    public ArrayList<BlockNode> childrenNodes;
    
    public BlockNode(Block block,BlockNode parentNode, UTXOPool utxoPool){
        this.block = block;
        this.parentNode = parentNode;
        this.utxoPool = utxoPool;
        this.childrenNodes = new ArrayList<BlockNode>();
        if (this.parentNode != null){
            this.height = this.parentNode.height + 1;
            this.parentNode.childrenNodes.add(this);
        }
        else{this.height = 1;}

    }
}
```

为每一个上链的block创建对应的node，属性包括：

- 该区块block
- 该blockNode的上一个节点parentNode
- 这个blockNode所对应的UTXOPool
- 这个blockNode的子节点childrenNodes
- 这个blockNode所处的链的位置height

### BlockChain

```java
blockChain = new HashMap<ByteArrayWrapper, BlockNode>();
```

block chain是一个hashMap，value是blockNode，key是blockNode.block.getHash()经过ByteArrayWrapper返回的值

这里初始化时，产生一个只含有创世区块节点genesisBlockNode的blockChain，并将maxHeightNode和oldestBlockHeight都设置为genesisNode

```java
blockChain.put(genesisHashWrap, genesisNode);
maxHeightNode = genesisNode;
oldestBlockHeight = genesisNode.height;
```

### addBlock

判断一个block是否valid

1. 如果这个block没有parent，或者说他构造的blockNode没有parentBlockNode，return false

   ```java
   if (block.getPrevBlockHash() == null) {
       System.out.println("Error: No parent block !");
       return (false);
   }
   ByteArrayWrapper prevBlockHashWrap = new ByteArrayWrapper(block.getPrevBlockHash());
   BlockNode parentNode = blockChain.get(prevBlockHashWrap);
   if (parentNode == null){
       System.out.println("Error: No parent block node !");
       return (false);
   }
   ```

2. 如果这个block里面的txs不是valid，return false

   ```java
   UTXOPool parentUTXOPool = parentNode.utxoPool;
   // validate txs in block
   Transaction[] possibleTxs = new Transaction[block.getTransactions().size()];
   for(int i=0;i<block.getTransactions().size();i++){
       possibleTxs[i] = block.getTransaction(i);
   }
   TxHandler txHandler = new TxHandler(parentUTXOPool);
   Transaction[] acceptedTxs = txHandler.handleTxs(possibleTxs);
   if (acceptedTxs.length < possibleTxs.length){
       System.out.println("Error: Some Txs in this block are invalid !");
       return (false);
   }
   ```

3. 如果这个block被添加在了离maxHeightNode有CUT_OFF_AGE的距离的位置，return false

   ```java
   // check the height of the block's parent
   if (parentNode.height + 1 <= maxHeightNode.height - CUT_OFF_AGE){
       System.out.println("Error: Height of the block !");
       return (false);
   }
   ```

4. block是valid之后，将其add到block Chain上，并更新整个TransactionPool，如果该block的height比maxHeightNode的height大，同时更新maxHeightNode.height

   ```java
   // create this blockNode using this block & parentNode & updatedUTXOPool
   BlockNode blockNode = new BlockNode(block,parentNode,updatedUTXOPool);
   // add this block into the blockChain
   blockChain.put(new ByteArrayWrapper(block.getHash()),blockNode);
   // update txPool
   for (Transaction tx: possibleTxs){
       txPool.removeTransaction(tx.getHash());
   }
   // update the maxHeightNode if blockNode's height is greater than maxHeightNode's height
   if (blockNode.height > maxHeightNode.height){
       maxHeightNode = blockNode;
   }
   ```

   

## BlockHandlerTest.java

1. testNullParentBlock
   检查genesisBlock（prevHash是null的block）是否可以被Block Handler process（assertFalse）
   并且print出 Error: No parent block !

2. testNullParentNodeBlock
   构造一个blockNode，他的parentNode不存在，检查是否可以被process（assertFalse）

3. testProcessTxs
   检查block Handler.processTx的功能，创建一个txA2BC，先不进行操作`blockHandler.processTx(txA2BC)`，此时看txPool里H（hashMap）的size，应该为0（没有将txA2BC加入global的txPool里面），再进行`blockHandler.processTx(txA2BC)`之后，H的size应该为1（说明成功addTransaction了）

4. testInvalidTxsBlock
   构造一个含有invalidTx的block，看handler能否检测出（assertFalse）

5. testMaxHeightBlock

   设置CUT_OFF_AGE=10，先构造一条blockChain，maxHeight=16，在height=2的block后面添加一个新的block，因为它的height为3与16相差大于CUT_OFF_AGE，所以应该是无效的。（assertFalse）

6. testMemory
   在BlockChain的class中设置NUM_RECENT_NODE=16，即最多存储16个block在内存中，这里创建20个block，当开始创建第16个block的时候，将会提示需要cut off一些block，通过验证最后blockChain.getOldestBlockHeight()==5 （21-16）来证明函数有效