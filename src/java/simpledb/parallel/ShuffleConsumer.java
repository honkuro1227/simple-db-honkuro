package simpledb.parallel;

import java.util.Iterator;
import simpledb.DbException;
import simpledb.OpIterator;
import simpledb.TransactionAbortedException;
import simpledb.Tuple;
import simpledb.TupleDesc;
import java.util.*;

/**
 * The consumer part of the Shuffle Exchange operator.
 * 
 * A ShuffleProducer operator sends tuples to all the workers according to some
 * PartitionFunction, while the ShuffleConsumer (this class) encapsulates the
 * methods to collect the tuples received at the worker from multiple source
 * workers' ShuffleProducer.
 * 
 * */
public class ShuffleConsumer extends Consumer {

    private static final long serialVersionUID = 1L;
    private OpIterator child;
    private ParallelOperatorID operatorID;
    private SocketInfo[] workers;
    private TupleDesc td;
    private BitSet workerEOS;
    private transient Iterator<Tuple> tuples;
    private final Map<String, Integer> workerIdToIndex;
    private transient int innerBufferIndex;
    private transient ArrayList<TupleBag> innerBuffer;
    public String getName() {
        return "shuffle_c";
    }

    public ShuffleConsumer(ParallelOperatorID operatorID, SocketInfo[] workers) {
        this(null, operatorID, workers);
    }

    public ShuffleConsumer(ShuffleProducer child, ParallelOperatorID operatorID, SocketInfo[] workers) {
        super(operatorID);
        // some code goes here
        this.child = child;
        this.workers = workers;
        if (child != null) {
            this.td = child.getTupleDesc();
        }
        workerIdToIndex = new HashMap<String, Integer>();
        int idx = 0;
        for (SocketInfo w : workers) {
            this.workerIdToIndex.put(w.getId(), idx++);
        }
        this.workerEOS = new BitSet(workers.length);
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        super.open();
        this.tuples = null;
        this.innerBuffer = new ArrayList<TupleBag>();
        this.innerBufferIndex = 0;
        if (this.child != null) {
            this.child.open();
        }
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        this.tuples = null;
        this.innerBufferIndex = 0;
    }

    @Override
    public void close() {
        // some code goes here
        super.close();
        this.setBuffer(null);
        this.tuples = null;
        this.innerBufferIndex = -1;
        this.innerBuffer = null;
        this.workerEOS.clear();
        if (child != null) {
            child.close();
        }
    }

    @Override
    public TupleDesc getTupleDesc() {
        // some code goes here
        if (child != null) {
            td = child.getTupleDesc();
        }
        return td;
    }

    /**
     * 
     * Retrieve a batch of tuples from the buffer of ExchangeMessages. Wait if
     * the buffer is empty.
     * 
     * @return Iterator over the new tuples received from the source workers.
     *         Return <code>null</code> if all source workers have sent an end
     *         of file message.
     */
    Iterator<Tuple> getTuples() throws InterruptedException {
        // some code goes here
        if (innerBufferIndex < innerBuffer.size()) {
            return this.innerBuffer.get(this.innerBufferIndex++).iterator();
        }
        while (this.workerEOS.nextClearBit(0) < this.workers.length) {
            TupleBag tb = (TupleBag) this.take(-1);
            if (tb.isEos()) {
                workerEOS.set(workerIdToIndex.get(tb.getWorkerID()));
            } else {
                innerBuffer.add(tb);
                innerBufferIndex++;
                return tb.iterator();
            }
        }
        return null;
    }

    @Override
    protected Tuple fetchNext() throws DbException, TransactionAbortedException {
        // some code goes here
        while (tuples == null || !tuples.hasNext()) {
            try {
                tuples = getTuples();
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
            if (tuples == null) {
                return null;
            }
        }
        return tuples.next();
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        this.child=children[0];
        td=child.getTupleDesc();
    }

}
