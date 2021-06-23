# Homework 1 

## TxHandler

### 初始化

```java
private UTXOPool uPool;
public TxHandler(UTXOPool utxoPool) {
    // IMPLEMENT THIS
    this.uPool = new UTXOPool(utxoPool);
    System.out.println("Create UTXOPool & TxHandler successfully!!");
}
```

- 输入：UTXOPool utxoPool
- 功能：创建一个UTXOPool uPool，make a copy of utxoPool
- print："Create UTXOPool & TxHandler successfully!!"

### isValidTx

根据五个要求，依次进行判断（对于每个input，output进行for loop），只要有一个不合格就return false，并且print出是Error（i）

1. all outputs claimed by {@code tx} are in the current UTXO pool

   ```java
   Transaction.Input in = tx.getInput(i);
   UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
   Transaction.Output prevTxOutput = uPool.getTxOutput(prevUTXO);
   if (prevTxOutput == null){
   	System.out.printf("Error(1): output claimed by input %d is not in the 			UTXOPool\n\n",i);
   return false;
   }
   ```

   通过input的两个属性new一个`UTXO prevUTXO`出来，然后在`uPool`中寻找对应的output，如果返回值是`null`，说明这个`prevUTXO`并不在`uPool`中，也就不满足第一个条件，返回false

2. the signatures on each input of {@code tx} are valid

   ```java
   byte[] message = tx.getRawDataToSign(i);
   PublicKey pubKey = prevTxOutput.address;
   byte[] sig = in.signature;
   if (Crypto.verifySignature(pubKey, message, sig) == false){
   	System.out.printf("Error(2): the signature of input %d is not valid\n\n",i);
       return false;
   }
   ```

   通过`Transaction.getRawDataToSign(i)`的方法，获取`message`，从`prevTxOutput`中拿到public key，从input里面拿到他的signature，输入到`Crypto.verifySignature(pubKey, message, sig)`中进行验证，如果返回值是false，则不是valid，返回false

3. no UTXO is claimed multiple times by {@code tx}

   ```java
   ArrayList<UTXO> UTXOs = new ArrayList<>();
   if (UTXOs.contains(prevUTXO)){
   	System.out.printf("Error(3): UXTO is claimed twice, inputIndex = %d\n\n",i);
   	return false;
   }
   UTXOs.add(prevUTXO);
   ```

   创建一个ArrayList UTXOs来记录这个transaction里面已经被验证为valid的input所claim的那些UTXO，如果现在正在检验的input所claim的prevUTXO已经存在于这个UTXOs中，说明遇到了两次，也就是double spending了，返回false

4. all of {@code tx}s output values are non-negative

   ```java
   for (int i = 0; i < tx.numOutputs(); i++){
   	Transaction.Output op = tx.getOutput(i);
   	if (op.value < 0){
   		System.out.printf("Error(4): value of output %d is %.3f and it's 				negative\n",i,op.value);
   		return false;
   	}
       valueSumOutputs += op.value;
   }
   ```

   对于每个output做for loop，检查他的value属性是否非负，如果他小于0，就返回false，否则将他的value加到valueSumOutputs中

5. the sum of {@code tx}s input values is greater than or equal to the sum of its output values; and false otherwise

   ```java
   double valueSumOutputs = 0.0;
   double valueSumInputs = 0.0;
   
   if (valueSumInputs < valueSumOutputs){
       System.out.printf(
           "Error(5): the sum of tx\'s input value is %.3f\nthe sum of tx\'s output 		value is %.3f\n" + "valueSumInputs < valueSumOutputs\n\n",
           valueSumInputs,
           valueSumOutputs
       );
       return false;
   }
   ```

   在一开始定义valueSumOutputs和valueSumInputs，在input和output的for loop中，将他们的value加上去，最后进行比较，如果valueSumInputs < valueSumOutputs，就返回false

### handleTxs

```java
public Transaction[] handleTxs(Transaction[] possibleTxs) {
    ArrayList<Integer> indexValid = new ArrayList<>();
    int indexValidLength;
    int indexValidLengthUpdate = 0;
    do {
        indexValidLength = indexValid.size();
        for (int i = 0; (i < possibleTxs.length) ; i++) {
            if (indexValid.contains(i)){
                continue;
            }
            Transaction tx = possibleTxs[i];
            if (isValidTx(tx)) {
                indexValid.add(i);
                updateUTXOPool(tx);
            }
        }
        indexValidLengthUpdate = indexValid.size();
    } while (indexValidLength < indexValidLengthUpdate);
    ArrayList<Transaction> validTxs = new ArrayList<>();
    for (int aIndexValid: indexValid){
        validTxs.add(possibleTxs[aIndexValid]);
    }
    Transaction[] validTxsArr = validTxs.toArray(new Transaction[validTxs.size()]);
    return validTxsArr;
}
```

- 利用indexValid记录possibleTxs里面被验证为valid的Tx在possibleTxs里的index
- 对于possibleTxs里的每个Tx作循环，如果他的index不在indexValid里面，且利用isValidTx方法被验证为valid，我们update现在的UTXOPool（这里另外写了一个方法`updateUTXOPool`）
- 利用indexValidLength和indexValidLengthUpdate记录UTXOPool被更新前后indexValid的长度，如果后者比前者大，说明这次for loop中，有对UTXOPool进行更新，那么再进行一次for loop，直到没有更新为止

## TxHandlerTest

1. testCreateLedger
   检查初始化有没有问题

2. testValidTxInPool
   创建Transaction B-->C，B的input是从Transaction A-->B中来的（记作utxoA2B），一开始不把utxoA2B放到uPool中，验证这个Tx是否valid（assertFalse），再将他加入uPool中，验证他是valid（assertTrue）

3. testValidTxSigns
   同上，这里在给input sign的时候，不用B的private，也就是输入一个错误的sign，验证Tx是否valid（assertFalse），之后再把sign改成正确的，验证他是valid（assertTrue）

4. testValidTxDoubleSpend
   检验是否有双花问题，构建Transaction B-->CD，B给C和D的input都是utxoA2B，验证这个Tx是否valid（assertFalse）

5. testValidOutputPos
   将output的value设置成-2，验证这个Tx是否valid（assertFalse）

6. testValidSumValue
   设置output的value的和大于input的value的和，验证这个Tx是否valid（assertFalse）

7. testHandleTxs
   设置Transaction[] possibleTxs，里面包含：

   - txC2BD：valid
   - txC2E：invalid（双花问题，用了和txC2BD一样的UTXO）
   - txD2BE：invalid（sumValueOutputs > sumValueInputs）
   - txB2DE：valid，是possibleTxs的第4个元素，用的UTXO来自最后一个txE2B的output
   - txE2B：valid

   最终输出的有效的validTxsArr应该包含（按顺序）txC2BD，txE2B和txB2DE

   所以assertEquals(validTxsArr.length,3)
