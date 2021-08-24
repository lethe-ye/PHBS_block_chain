// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    // keep the recent 10 nodes in memory
    public static final int NUM_RECENT_NODE = 16;
    // IMPLEMENT THIS
    private int oldestBlockHeight;
    private HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private TransactionPool txPool;
    private BlockNode maxHeightNode;

    /**
     * for each block, create a corresponding node,
     * containing infomation of the block itself, the block's parent node and utxoPool corresponding to this block
     * when create a children BlockNode, the utxoPool of its parentNode should be updated using
     * the txs in this children BlockNode
     */
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
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        blockChain = new HashMap<ByteArrayWrapper, BlockNode>();
        txPool = new TransactionPool();

        ByteArrayWrapper genesisHashWrap = new ByteArrayWrapper(genesisBlock.getHash());
        BlockNode genesisNode = new BlockNode(genesisBlock,null, new UTXOPool());
        // update utxoPool: add the outputs of coinbaseTx into genesisNode.utxoPool
        Transaction coinbaseTx = genesisBlock.getCoinbase();
        byte[] coinbaseTxHash = coinbaseTx.getHash();
        for (int i = 0;i<coinbaseTx.numOutputs();i++){
            Transaction.Output output = coinbaseTx.getOutput(i);
            UTXO utxo = new UTXO(coinbaseTxHash,i);
            genesisNode.utxoPool.addUTXO(utxo,output);
        }
        // add genesisNode to this.blockChain
        blockChain.put(genesisHashWrap, genesisNode);
        // TODO: update txPool ??
        // this is done in BlockHandler
//        txPool.addTransaction(coinbaseTx);
        // init the maxHeightNode as genesisNode
        maxHeightNode = genesisNode;
        oldestBlockHeight = genesisNode.height;

    }

    public int getOldestBlockHeight(){
        return oldestBlockHeight;
    }

    public int getMaxHeight(){
        return maxHeightNode.height;
    }
    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return maxHeightNode.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // check whether the block's parent is null
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
        // check the height of the block's parent
        if (parentNode.height + 1 <= maxHeightNode.height - CUT_OFF_AGE){
            System.out.println("Error: Height of the block !");
            return (false);
        }
        // get the updated utxoPool from txHandler and add coinbase's output into this utxoPool
        UTXOPool updatedUTXOPool = txHandler.getUTXOPool();
        for (int i = 0; i < block.getCoinbase().numOutputs(); i++){
            Transaction.Output output = block.getCoinbase().getOutput(i);
            UTXO utxo = new UTXO(block.getCoinbase().getHash(),i);
            updatedUTXOPool.addUTXO(utxo,output);
        }
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
        // just keep the recent 16 nodes in memory (NUM_RECENT_NODE=16)
        if (maxHeightNode.height - oldestBlockHeight > NUM_RECENT_NODE){
            System.out.println("need to cut off some blocks in memory");
            Iterator<ByteArrayWrapper> blockHashWrapIter = blockChain.keySet().iterator();
            while(blockHashWrapIter.hasNext()){
                ByteArrayWrapper nextBlockHashWrap = blockHashWrapIter.next();
                BlockNode nextBlockNode = blockChain.get(nextBlockHashWrap);
                if (maxHeightNode.height - nextBlockNode.height > NUM_RECENT_NODE){
                    blockHashWrapIter.remove();
                }
            }
            System.out.println("Original oldestBlockHeight is "+oldestBlockHeight);
            oldestBlockHeight = maxHeightNode.height - NUM_RECENT_NODE;
            System.out.println("After cutting off, new oldestBlockHeight is "+oldestBlockHeight+"\n");
        }
        return (true);


    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
    }
}