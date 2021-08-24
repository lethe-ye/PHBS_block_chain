import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.*;

import static org.junit.jupiter.api.Assertions.*;

class BlockHandlerTest {
    static KeyPair keyPairA, keyPairB, keyPairC, keyPairD, keyPairE;
    static KeyPairGenerator keyPairGen;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairA = keyPairGen.generateKeyPair();
        keyPairB = keyPairGen.generateKeyPair();
        keyPairC = keyPairGen.generateKeyPair();
        keyPairD = keyPairGen.generateKeyPair();
        keyPairE = keyPairGen.generateKeyPair();

    }


    @Test
    /**
     * check whether the blockHandler can process an empty block (the prevHash is null)
     * so we can use the genesisBlock as the test block
     * blockB use genesisBlock as its prevBlock and should be processed successfully
     */
    void testNullParentBlock(){
        // create a genesis block whose miner is A
        Block genesisBlock = new Block(null, keyPairA.getPublic());
        genesisBlock.finalize();
        // init the block chain and block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        // B mine the next block
        Block blockB = new Block(genesisBlock.getHash(), keyPairB.getPublic());
        blockB.finalize();
        // test whether blockHandler can process genesisBlock(false) & blockB(True)
        assertFalse(blockHandler.processBlock(genesisBlock));
        assertTrue(blockHandler.processBlock(blockB));
    }

    @Test
    /**
     * here, we construct blockB with a wrong prevBlockHash which should have been the hash of the genesis block
     * and this will result in a null parentBlockNode
     */
    void testNullParentNodeBlock(){
        // create a genesis block whose miner is A
        Block genesisBlock = new Block(null, keyPairA.getPublic());
        genesisBlock.finalize();
        // init the block chain and block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        // B mine the next block
        byte[] genesisBlockHash = genesisBlock.getHash();
        genesisBlockHash[0]++;
        // input a wrong prevBlockHash
        // the blockB's parentBlockNode should be null
        Block blockB = new Block(genesisBlockHash, keyPairB.getPublic());
        blockB.finalize();
        // test whether blockHandler can process blockB(False)
        assertFalse(blockHandler.processBlock(blockB));

    }

    @Test
    /**
     * test processTx without blockHandler.processTx(txA2BC)
     * there is no tx in the global txPool
     */
    void testProcessTxs() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // create a genesis block whose miner is A
        Block genesisBlock = new Block(null, keyPairA.getPublic());
        genesisBlock.finalize();
        // init the block chain and block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block blockB = new Block(genesisBlock.getHash(), keyPairB.getPublic());
        // construct an invalid txA2BC: output value(15+20) > input value(block reward:25)
        Transaction txA2BC = new Transaction();
        txA2BC.addInput(genesisBlock.getCoinbase().getHash(),0);;
        txA2BC.addOutput(15,keyPairB.getPublic());
        txA2BC.addOutput(20, keyPairC.getPublic());
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairA.getPrivate());
        sign.update(txA2BC.getRawDataToSign(0));
        txA2BC.addSignature(sign.sign(), 0);
        txA2BC.finalize();
        // while blockHandler doesn't process this txA2BC
        // there is no tx in txPool
        assertTrue(blockChain.getTransactionPool().getTransactionPoolSize() == 0);
        blockHandler.processTx(txA2BC);
        assertTrue(blockChain.getTransactionPool().getTransactionPoolSize() == 1);
    }


    @Test
    /**
     * here, we construct a block which contains invalid txs
     * check whether this block can be processed (false)
     */
    void testInvalidTxsBlock() throws InvalidKeyException, SignatureException, NoSuchAlgorithmException {
        // create a genesis block whose miner is A
        Block genesisBlock = new Block(null, keyPairA.getPublic());
        genesisBlock.finalize();
        // init the block chain and block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        Block blockB = new Block(genesisBlock.getHash(), keyPairB.getPublic());
        // construct an invalid txA2BC: output value(15+20) > input value(block reward:25)
        Transaction txA2BC = new Transaction();
        txA2BC.addInput(genesisBlock.getCoinbase().getHash(),0);;
        txA2BC.addOutput(15,keyPairB.getPublic());
        txA2BC.addOutput(20, keyPairC.getPublic());
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairA.getPrivate());
        sign.update(txA2BC.getRawDataToSign(0));
        txA2BC.addSignature(sign.sign(), 0);
        txA2BC.finalize();
        blockHandler.processTx(txA2BC);
//        blockChain.addTransaction(txA2BC);
        // add txA2BC to blockB and check whether blockHandler can process this blockB
        blockB.addTransaction(txA2BC);
        blockB.finalize();
        // should print "Error: Some Txs in this block are invalid !"
        assertFalse(blockHandler.processBlock(blockB));

    }

    @Test
    /**
     * here, we construct a block chain with max height = 14
     * then, we create a block whose parentBlockNode's height is 2
     * check whether this block can be processed (false)
     */
    void testMaxHeightBlock(){
        // create a genesis block whose miner is A
        Block genesisBlock = new Block(null, keyPairA.getPublic());
        genesisBlock.finalize();
        // init the block chain and block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        // add 14 blocks to this block chain
        Block block1 = new Block(genesisBlock.getHash(), keyPairB.getPublic());
        block1.finalize();
        for(int i = 0;i<13;i++){
            Block blocki = new Block(blockChain.getMaxHeightBlock().getHash(), keyPairB.getPublic());
            blocki.finalize();
            blockChain.addBlock(blocki);
        }
        // add a block whose parentBlockNode.height = 2
        // this block cannot be processed (height is smaller than maxHeighNode.height - CUT_OFF_AGE)
        Block block2 = new Block(block1.getHash(), keyPairC.getPublic());
        assertFalse(blockHandler.processBlock(block2));

    }

    @Test
    /**
     * check whether it can cut off some blocks to save memory
     * we set NUM_RECENT_BLOCK as 16 which means only 16 blocks can be saved in memory
     * During constructing 21 blocks in the blockChain
     * when we start to create the 17th block
     * the chain should cut off a block from the memory and set the oldestBlockHeight as 1+1
     * then 2+1 3+1 and 4+1=5
     */
    void testMemory(){
        // create a genesis block whose miner is A
        Block genesisBlock = new Block(null, keyPairA.getPublic());
        genesisBlock.finalize();
        // init the block chain and block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);
        for(int i = 0;i<20;i++){
            Block blocki = blockHandler.createBlock(keyPairB.getPublic());
        }
        // there are 21 blocks in this chain
        // after cutting off 16 blocks in memory, the oldestBlockHeight should be
        // 21 - 16 = 5
        assertTrue(blockChain.getOldestBlockHeight()==5);

    }
}