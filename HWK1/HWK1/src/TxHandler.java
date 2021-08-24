import java.awt.*;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.TimerTask;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool uPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.uPool = new UTXOPool(utxoPool);
        System.out.println("Create UTXOPool & TxHandler successfully!!");
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // (5) store the value of inputs & outputs of the tx
        double valueSumOutputs = 0.0;
        double valueSumInputs = 0.0;
        // (3) store the utxo in each iteration
        ArrayList<UTXO> UTXOs = new ArrayList<>();

        for (int i = 0; i < tx.numInputs(); i++){
            // (1)
            // for each input in tx, construct an UTXO prevUTXO
            // using the prevTxHash and outputIndex of the input
            // and verify whether prevUTXO is in the uPool
            Transaction.Input in = tx.getInput(i);
            UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output prevTxOutput = uPool.getTxOutput(prevUTXO);
            if (prevTxOutput == null){
                System.out.printf("Error(1): output claimed by input %d is not in the UTXOPool\n\n",i);
                return false;
            }
            // (2)
            // get the message, public key and sign
            // to check the validation of the input
            byte[] message = tx.getRawDataToSign(i);
            PublicKey pubKey = prevTxOutput.address;
            byte[] sig = in.signature;
            if (Crypto.verifySignature(pubKey, message, sig) == false){
                System.out.printf("Error(2): the signature of input %d is not valid\n\n",i);
                return false;
            }
            // (3)
            // record each prevUTXO in UTXOs
            // check whether prevUTXO already exist in UTXOs
            if (UTXOs.contains(prevUTXO)){
                System.out.printf("Error(3): UXTO is claimed twice, inputIndex = %d\n\n",i);
                return false;
            }
            UTXOs.add(prevUTXO);
            // (5)
            valueSumInputs += prevTxOutput.value;
        }

        for (int i = 0; i < tx.numOutputs(); i++){
            Transaction.Output op = tx.getOutput(i);
            // (4)
            // check the value of outputs is positive or not
            if (op.value < 0){
                System.out.printf("Error(4): value of output %d is %.3f and it's negative\n",i,op.value);
                return false;
            }
            // (5)
            valueSumOutputs += op.value;
        }

        // (5)
        if (valueSumInputs < valueSumOutputs){
            System.out.printf(
                    "Error(5): the sum of tx\'s input value is %.3f\nthe sum of tx\'s output value is %.3f\n" +
                            "valueSumInputs < valueSumOutputs\n\n",
                    valueSumInputs,
                    valueSumOutputs
            );
            return false;
        }
        return true;
    }

    /**
     * if {@param tx} is validated, we use the updateUTXOPool
     * to update our {@code uPool} by the information in {@param tx}
     * @param tx
     */
    public void updateUTXOPool(Transaction tx){
        // remove the outputs claimed by the tx
        for (int i = 0; i < tx.numInputs(); i++){
            Transaction.Input in = tx.getInput(i);
            UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
            this.uPool.removeUTXO(prevUTXO);
        }
        for (int i = 0; i < tx.numOutputs(); i++){
            Transaction.Output op = tx.getOutput(i);
            byte[] txHash = tx.getHash();
            UTXO toAddUTXO = new UTXO(txHash,i);
            this.uPool.addUTXO(toAddUTXO,op);
        }
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        // record the indexes of valid txs
        ArrayList<Integer> indexValid = new ArrayList<>();
        // record the length of indexValid before and after the update of UTXOPool
        // if indexValidLength < indexValidLengthUpdate
        // uPool is updated by some txs in possibleTxs and we add the index of these txs into indexValid
        int indexValidLength;
        int indexValidLengthUpdate = 0;
        do {
            // record the length of indexValid before
            indexValidLength = indexValid.size();
            // for loop: check the validity of each of the tx in possibleTxs
            for (int i = 0; (i < possibleTxs.length) ; i++) {
                if (indexValid.contains(i)){
                    continue;
                }
                Transaction tx = possibleTxs[i];
                // if tx is valid, we add its index into the indexValid
                // and updateUTXOPool using the tx's information
                if (isValidTx(tx)) {
                    indexValid.add(i);
                    updateUTXOPool(tx);
                }
            }
            // after the loop, we check the length of indexValid
            // if indexValidLength < indexValidLengthUpdate
            // do the for loop again until there is no updating
            indexValidLengthUpdate = indexValid.size();
        } while (indexValidLength < indexValidLengthUpdate);
        // collect valid txs from the possibleTxs
        // store them in validTxs and convert arraylist to array
        // return validTxsArr
        ArrayList<Transaction> validTxs = new ArrayList<>();
        for (int aIndexValid: indexValid){
            validTxs.add(possibleTxs[aIndexValid]);
        }
        Transaction[] validTxsArr = validTxs.toArray(new Transaction[validTxs.size()]);

        return validTxsArr;
    }
}
