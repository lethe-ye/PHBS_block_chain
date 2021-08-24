import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


class TxHandlerTest {

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

    @AfterEach
    void tearDown() {
    }

    @Test
    /**
     * test whether we can create the ledger successfully
     */
    void testCreateLedger(){
        UTXOPool testUTXOPool = new UTXOPool();
        TxHandler testHandler = new TxHandler(testUTXOPool);
        assertNotNull(testHandler);
    }

    @Test
    /**
     * test Error 1
     * first, create an empty pool, meaning this pool doesn't contain the utxo claimed by the tx
     * assertFalse(txHandler.isValidTx(txB2CD)) (should pass)
     * then, add the corresponding utxo into the pool
     * assertTrue(txHandler.isValidTx(txB2CD)) (should pass)
     *
     * first: A --> B (in the UTXOPool)
     * then: B --> C & D (the tx we want to check the validity)
     */
    void testValidTxInPool() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // create an empty UTXOPool
        UTXOPool currUTXOPool = new UTXOPool();
        // here, the currUTXOPool doesn't contain the utxoA2B
        // thus, txB2CD which uses the output of txA2B (utxoA2B) is invalid
        TxHandler testHandler = new TxHandler(currUTXOPool);

        // in txA2B A --> B ($10), output should
        // value = 10, address = B's public key
        Transaction txA2B = new Transaction();
        txA2B.addOutput(10, keyPairB.getPublic());
        txA2B.finalize();
        // create corresponding UTXO, the outputIndex is 0
        UTXO utxoA2B = new UTXO(txA2B.getHash(),0);

        // in txB2CD B($10) --> C($5+txFee?) D($3+txFee?)
        Transaction txB2CD = new Transaction();
        // the inputIndex is 0
        txB2CD.addInput(utxoA2B.getTxHash(),0);
        // outputIndex = 0
        txB2CD.addOutput(5, keyPairC.getPublic());
        // outputIndex = 1
        txB2CD.addOutput(3, keyPairD.getPublic());
        // add sign to inputIndex = 0
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairB.getPrivate());
        sign.update(txB2CD.getRawDataToSign(0));
        txB2CD.addSignature(sign.sign(), 0);
        txB2CD.finalize();

        // currUTXOPool doesn't contain utxoA2B
        // should print
        // "Error(1): output claimed by input 0 is not in the UTXOPool"
        assertFalse(testHandler.isValidTx(txB2CD));

        // add utxoA2B into the currUTXOPool and create a new handler
        // this time there should not be Error(1)
        currUTXOPool.addUTXO(utxoA2B,txA2B.getOutput(0));
        TxHandler testHandlerNew = new TxHandler(currUTXOPool);
        assertTrue(testHandlerNew.isValidTx(txB2CD));

    }

    @Test
    /** test Error 2
     * the signatures on each input of {@code tx} are valid,
     * first, make the sign of C in txBC2D invalid
     * getRawDataToSign (index should be 1 but we set it as 0)
     * assertFalse(txHandler.isValidTx(txBC2D))
     * (should pass and print "Error(2): the signature of input 1 is not valid")
     * then, set the index as 1 (make the sign of C valid)
     * assertTrue(txHandler.isValidTx(txBC2D)) (should pass)
     *
     * first, txA2BC A2B($10, index=0) A2C($20, index=1)
     * then, txBC2D($30) (to check signs of B & C are valid)
     */
    void testValidTxSigns() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        UTXOPool currUTXOPool = new UTXOPool();

        // make txA2BC and add create the TxHandler
        Transaction txA2BC = new Transaction();
        txA2BC.addOutput(10, keyPairB.getPublic());
        txA2BC.addOutput(20, keyPairC.getPublic());
        txA2BC.finalize();
        UTXO utxoA2B = new UTXO(txA2BC.getHash(),0);
        UTXO utxoA2C = new UTXO(txA2BC.getHash(),1);
        currUTXOPool.addUTXO(utxoA2B,txA2BC.getOutput(0));
        currUTXOPool.addUTXO(utxoA2C,txA2BC.getOutput(1));
        TxHandler testHandler = new TxHandler(currUTXOPool);

        // make txBC2D
        Transaction txBC2D = new Transaction();
        txBC2D.addInput(utxoA2B.getTxHash(),0);
        txBC2D.addInput(utxoA2C.getTxHash(),1);
        txBC2D.addOutput(30,keyPairD.getPublic());
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairB.getPrivate());
        sign.update(txBC2D.getRawDataToSign(0));
        txBC2D.addSignature(sign.sign(), 0);
        sign.initSign(keyPairC.getPrivate());
        // use invalid sign of C (valid: index should be 1)
        sign.update(txBC2D.getRawDataToSign(0));
        txBC2D.addSignature(sign.sign(), 1);
        txBC2D.finalize();
        // should print
        // "Error(2): the signature of input 1 is not valid"
        assertFalse(testHandler.isValidTx(txBC2D));

        sign.initSign(keyPairC.getPrivate());
        // use valid sign of C (set index = 1)
        sign.update(txBC2D.getRawDataToSign(1));
        txBC2D.addSignature(sign.sign(), 1);
        txBC2D.finalize();

        assertTrue(testHandler.isValidTx(txBC2D));

    }


    @Test
    /** test Error 3
     * no UTXO is claimed multiple times by {@code tx}, double spending
     * first txA2B($10)
     * then txB2CD(B-->C $10; B-->D $10) invalid
     */
    void testValidTxDoubleSpend() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // create an empty UTXOPool
        UTXOPool currUTXOPool = new UTXOPool();

        Transaction txA2B = new Transaction();
        txA2B.addOutput(10, keyPairB.getPublic());
        txA2B.finalize();
        UTXO utxoA2B = new UTXO(txA2B.getHash(),0);
        currUTXOPool.addUTXO(utxoA2B,txA2B.getOutput(0));
        TxHandler testHandler = new TxHandler(currUTXOPool);

        // make txB2CD double spending
        Transaction txB2CD = new Transaction();
        txB2CD.addInput(utxoA2B.getTxHash(),0);
        txB2CD.addInput(utxoA2B.getTxHash(),0);
        txB2CD.addOutput(10, keyPairC.getPublic());
        txB2CD.addOutput(10, keyPairD.getPublic());
        // make signs
        // since the two signs use rawData from inputs with the same utxo
        // their signs should be the same
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairB.getPrivate());
        sign.update(txB2CD.getRawDataToSign(0));
        txB2CD.addSignature(sign.sign(), 0);
        sign.initSign(keyPairB.getPrivate());
        sign.update(txB2CD.getRawDataToSign(1));
        txB2CD.addSignature(sign.sign(), 1);
        txB2CD.finalize();
        // should print
        // "Error(3): UXTO is claimed twice, inputIndex = 1"
        assertFalse(testHandler.isValidTx(txB2CD));

        // TODO: fix the bug here
        // remove input & output of txB2D B-->D($10)
        // should be valid???
//        txB2CD.removeInput(1);
//        txB2CD.removeOutput(1);
//        txB2CD.finalize();
//        assertTrue(testHandler.isValidTx(txB2CD));

    }

    @Test
    /**
     * test Error 4
     * all of {@code tx}s output values are non-negative
     */
    void testValidOutputPos() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // create an empty UTXOPool
        UTXOPool currUTXOPool = new UTXOPool();

        Transaction txA2B = new Transaction();
        txA2B.addOutput(10, keyPairB.getPublic());
        txA2B.finalize();
        UTXO utxoA2B = new UTXO(txA2B.getHash(),0);
        currUTXOPool.addUTXO(utxoA2B,txA2B.getOutput(0));
        TxHandler testHandler = new TxHandler(currUTXOPool);

        // in txB2CD B($10) --> C($8+txFee?) D($-2+txFee?)
        Transaction txB2CD = new Transaction();
        // the inputIndex is 0
        txB2CD.addInput(utxoA2B.getTxHash(),0);
        // outputIndex = 0
        txB2CD.addOutput(8, keyPairC.getPublic());
        // outputIndex = 1
        txB2CD.addOutput(-2, keyPairD.getPublic());
        // add sign to inputIndex = 0
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairB.getPrivate());
        sign.update(txB2CD.getRawDataToSign(0));
        txB2CD.addSignature(sign.sign(), 0);
        txB2CD.finalize();

        // outputs[1].value = -2, negative
        // should print
        // "Error(4): value of output 1 is negative"
        assertFalse(testHandler.isValidTx(txB2CD));
    }

    @Test
    /**
     * test Error 5
     * the sum of {@code tx}s input values is greater than or equal to the sum of its output values
     * first, txA2BC: A-->B($10) A-->C($10)
     * then, txBC2DE: B-->D(input$10,output$15) C-->E($10)
     * sumValueOut=$25 sumValueIn=$20
     */
    void testValidSumValue() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        UTXOPool currUTXOPool = new UTXOPool();

        // make txA2BC and add create the TxHandler
        Transaction txA2BC = new Transaction();
        txA2BC.addOutput(10, keyPairB.getPublic());
        txA2BC.addOutput(10, keyPairC.getPublic());
        txA2BC.finalize();
        UTXO utxoA2B = new UTXO(txA2BC.getHash(),0);
        UTXO utxoA2C = new UTXO(txA2BC.getHash(),1);
        currUTXOPool.addUTXO(utxoA2B,txA2BC.getOutput(0));
        currUTXOPool.addUTXO(utxoA2C,txA2BC.getOutput(1));
        TxHandler testHandler = new TxHandler(currUTXOPool);

        Transaction txBC2DE = new Transaction();
        // B-->D
        txBC2DE.addInput(utxoA2B.getTxHash(),0);
        // output value is 15 > 10 (input value)
        txBC2DE.addOutput(15, keyPairD.getPublic());
        // C-->E
        txBC2DE.addInput(utxoA2C.getTxHash(),1);
        txBC2DE.addOutput(10, keyPairE.getPublic());

        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(keyPairB.getPrivate());
        sign.update(txBC2DE.getRawDataToSign(0));
        txBC2DE.addSignature(sign.sign(), 0);
        sign.initSign(keyPairC.getPrivate());
        sign.update(txBC2DE.getRawDataToSign(1));
        txBC2DE.addSignature(sign.sign(), 1);
        txBC2DE.finalize();

        // should print
        // "Error(5): the sum of tx's input value is 20.000"
        // "the sum of tx's output value is 25.000"
        // "valueSumInputs < valueSumOutputs"
        assertFalse(testHandler.isValidTx(txBC2DE));
    }

    @Test
    void testHandleTxs() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        UTXOPool currUTXOPool = new UTXOPool();

        // make txA2BC, txA2DE and add create the TxHandler
        Transaction txA2BC = new Transaction();
        txA2BC.addOutput(10, keyPairB.getPublic());
        txA2BC.addOutput(10, keyPairC.getPublic());
        txA2BC.finalize();
        UTXO utxoA2B = new UTXO(txA2BC.getHash(),0);
        UTXO utxoA2C = new UTXO(txA2BC.getHash(),1);
        currUTXOPool.addUTXO(utxoA2B,txA2BC.getOutput(0));
        currUTXOPool.addUTXO(utxoA2C,txA2BC.getOutput(1));

        Transaction txA2DE = new Transaction();
        txA2DE.addOutput(30, keyPairD.getPublic());
        txA2DE.addOutput(30, keyPairE.getPublic());
        txA2DE.finalize();
        UTXO utxoA2D = new UTXO(txA2DE.getHash(),0);
        UTXO utxoA2E = new UTXO(txA2DE.getHash(),1);
        currUTXOPool.addUTXO(utxoA2D,txA2DE.getOutput(0));
        currUTXOPool.addUTXO(utxoA2E,txA2DE.getOutput(1));

        TxHandler testHandler = new TxHandler(currUTXOPool);

        // make txC2BD, txC2E(double spending,invalid)
        // txD2BE(sumValueOutputs>sumValueInputs,invalid)
        // txE2B, and txB2CD (use the output of txE2B)

        Signature sign = Signature.getInstance("SHA256withRSA");

        // txC2BD --> valid
        Transaction txC2BD = new Transaction();
        txC2BD.addInput(utxoA2C.getTxHash(),1);
        txC2BD.addOutput(7, keyPairB.getPublic());
        txC2BD.addOutput(3, keyPairD.getPublic());
        sign.initSign(keyPairC.getPrivate());
        sign.update(txC2BD.getRawDataToSign(0));
        txC2BD.addSignature(sign.sign(), 0);
        txC2BD.finalize();


        // txC2E double spending (txC2BD) --> invalid
        Transaction txC2E = new Transaction();
        txC2E.addInput(utxoA2C.getTxHash(), 1);
        txC2E.addOutput(10, keyPairE.getPublic());
        sign.initSign(keyPairC.getPrivate());
        sign.update(txC2E.getRawDataToSign(0));
        txC2E.addSignature(sign.sign(), 0);
        txC2E.finalize();


        // txD2BE sumValueOutputs > sumValueInputs --> invalid
        Transaction txD2BE = new Transaction();
        txD2BE.addInput(utxoA2D.getTxHash(),0);
        txD2BE.addOutput(20, keyPairB.getPublic());
        txD2BE.addOutput(20, keyPairE.getPublic());
        sign.initSign(keyPairD.getPrivate());
        sign.update(txD2BE.getRawDataToSign(0));
        txD2BE.addSignature(sign.sign(), 0);
        txD2BE.finalize();


        // txE2B --> valid
        Transaction txE2B = new Transaction();
        txE2B.addInput(utxoA2E.getTxHash(), 1);
        txE2B.addOutput(30, keyPairB.getPublic());
        sign.initSign(keyPairE.getPrivate());
        sign.update(txE2B.getRawDataToSign(0));
        txE2B.addSignature(sign.sign(), 0);
        txE2B.finalize();
        UTXO utxoE2B = new UTXO(txE2B.getHash(),0);


        // txB2DE (use utxo from txE2B in possibleTxs) --> valid
        Transaction txB2DE = new Transaction();
        txB2DE.addInput(utxoE2B.getTxHash(),0);
        txB2DE.addOutput(15, keyPairD.getPublic());
        txB2DE.addOutput(15, keyPairE.getPublic());
        sign.initSign(keyPairB.getPrivate());
        sign.update(txB2DE.getRawDataToSign(0));
        txB2DE.addSignature(sign.sign(), 0);
        txB2DE.finalize();

        // possibleTxs is an unordered list
        // so we put the txB2DE before txE2B
        Transaction[] possibleTxs = new Transaction[]{
                txC2BD, // valid
                txC2E, // invalid, double spending (txC2BD uses the same utxo) print no utxo (UTXOPool is updated)
                txD2BE, // invalid, sumValueOutputs > sumValueInputs
                txB2DE, // valid, use utxo from the next txE2B
                txE2B // valid, its output is used by txB2DE
        };

        Transaction[] validTxsArr = testHandler.handleTxs(possibleTxs);
        assertEquals(validTxsArr.length,3);
//        possibleTxs.add(txC2BD);
//        possibleTxs.add(txC2E);
//        possibleTxs.add(txD2BE);
//        possibleTxs.add(txB2DE);
//        possibleTxs.add(txE2B);

    }
}