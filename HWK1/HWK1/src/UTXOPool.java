import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class UTXOPool {

    /**
     * The current collection of UTXOs, with each one mapped to its corresponding transaction output
     */
    /**
     * 提前声明 H 的类型
     * H是UTXO和产生这个UTXO的output script的一一对应
     * H是UTXO和产生这个UTXO的output script的一一对应
     */
    private HashMap<UTXO, Transaction.Output> H;

    /** Creates a new empty UTXOPool
     * 如果什么都不输，就create一个空的pool
     * 也可以输入一个UTXOPool，就会以这个的copy作为初始的Pool
     * 系统内有更新UTXO的时候，就要使用addUTXO，把UTXO和它对应的output script都加进去
     * 如果这个UTXO被使用了，就使用 removeUTXO 从这个Pool里面把它删除（相当于dict里面找到key把内容删掉）
     */
    public UTXOPool() {
        H = new HashMap<UTXO, Transaction.Output>();
    }

    /** Creates a new UTXOPool that is a copy of {@code uPool} */
    public UTXOPool(UTXOPool uPool) {
        H = new HashMap<UTXO, Transaction.Output>(uPool.H);
    }

    /** Adds a mapping from UTXO {@code utxo} to transaction output @code{txOut} to the pool */
    public void addUTXO(UTXO utxo, Transaction.Output txOut) {
        H.put(utxo, txOut);
    }

    /** Removes the UTXO {@code utxo} from the pool */
    public void removeUTXO(UTXO utxo) {
        H.remove(utxo);
    }

    /**
     * @return the transaction output corresponding to UTXO {@code utxo}, or null if {@code utxo} is
     *         not in the pool.
     */
    public Transaction.Output getTxOutput(UTXO ut) {
        return H.get(ut);
    }

    /** @return true if UTXO {@code utxo} is in the pool and false otherwise */
    public boolean contains(UTXO utxo) {
        return H.containsKey(utxo);
    }

    /** Returns an {@code ArrayList} of all UTXOs in the pool */
    public ArrayList<UTXO> getAllUTXO() {
        Set<UTXO> setUTXO = H.keySet();
        ArrayList<UTXO> allUTXO = new ArrayList<UTXO>();
        for (UTXO ut : setUTXO) {
            allUTXO.add(ut);
        }
        return allUTXO;
    }
}
