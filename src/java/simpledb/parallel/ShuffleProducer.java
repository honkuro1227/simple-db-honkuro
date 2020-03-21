package simpledb.parallel;


import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import simpledb.*;
import simpledb.OpIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The producer part of the Shuffle Exchange operator.
 *
 * ShuffleProducer distributes tuples to the workers according to some
 * partition function (provided as a PartitionFunction object during the
 * ShuffleProducer's instantiation).
 * Anupam Gupta
 * */
public class ShuffleProducer extends Producer {

    private static final long serialVersionUID = 1L;
    private OpIterator child;
    private ParallelOperatorID operatorID;
    private SocketInfo[] workers;
    private PartitionFunction<?, ?> pf;
    private WorkingThread runningThread;

    public String getName() {
        return "shuffle_p";
    }

    public ShuffleProducer(OpIterator child, ParallelOperatorID operatorID,
                           SocketInfo[] workers, PartitionFunction<?, ?> pf) {
        super(operatorID);
        this.child = child;
        this.operatorID = operatorID;
        this.workers = workers;
        this.pf = pf;
    }

    public void setPartitionFunction(PartitionFunction<?, ?> pf) {
        this.pf = pf;
    }

    public SocketInfo[] getWorkers() {
        return this.workers;
    }

    public PartitionFunction<?, ?> getPartitionFunction() {
        return this.pf;
    }


    // some code goes here
    class WorkingThread extends Thread {
        IoSession[] cosessions = new IoSession[workers.length];
        Map<String, ArrayList<Tuple>> WorkerIdToBuffer = new HashMap<String, ArrayList<Tuple>>();

        public void run() {
            for(int i = 0; i <  workers.length; i++) {
                IoSession session = ParallelUtility.createSession(workers[i].getAddress(), getThisWorker().minaHandler, -1);
                WorkerIdToBuffer.put(workers[i].getId(), new ArrayList<Tuple>());
                cosessions[i] = session;
            }
            try {
                long latest = System.currentTimeMillis();
                while (child.hasNext()) {
                    Tuple tuple = child.next();
                    int partitionValue = pf.partition(tuple, getTupleDesc());
                    SocketInfo consumerWorker = workers[partitionValue];
                    ArrayList<Tuple> buffer = WorkerIdToBuffer.get(consumerWorker.getId());
                    buffer.add(tuple);

                    if (buffer.size() >= TupleBag.MAX_SIZE) {
                        IoSession session = cosessions[partitionValue];
                        session.write(new TupleBag(
                                operatorID,
                                getThisWorker().workerID,
                                buffer.toArray(new Tuple[]{}),
                                getTupleDesc()
                        ));
                        buffer.clear();
                        latest = System.currentTimeMillis();
                    }
                    if (buffer.size() >= TupleBag.MIN_SIZE) {
                        long now = System.currentTimeMillis();
                        if (now - latest > TupleBag.MAX_MS) {
                            IoSession session = cosessions[partitionValue];
                            session.write(new TupleBag(
                                    operatorID,
                                    getThisWorker().workerID,
                                    buffer.toArray(new Tuple[]{}),
                                    getTupleDesc()
                            ));
                            buffer.clear();
                            latest = now;
                        }
                    }
                }
                IoFutureListener<WriteFuture> something = new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture ioFuture) {
                        ParallelUtility.closeSession(ioFuture.getSession());
                    }
                };
                for(int i = 0; i < workers.length; i++) {
                    ArrayList<Tuple> buffer = WorkerIdToBuffer.get(workers[i].getId());
                    if(buffer.size() > 0) {
                        cosessions[i].write(new TupleBag(
                                operatorID,
                                getThisWorker().workerID,
                                buffer.toArray(new Tuple[]{}),
                                getTupleDesc()
                        ));
                    }
                    cosessions[i].write(new TupleBag(operatorID, getThisWorker().workerID)).addListener( something);
                }
            } catch (DbException e) {
                e.printStackTrace();
                System.out.println("DbException thrown by Shuffle Producer " + e.getLocalizedMessage());
            } catch (TransactionAbortedException e) {
                e.printStackTrace();
                System.out.println("TransactionAbortedException thrown by Shuffle Producer = " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        this.child.open();
        this.runningThread = new WorkingThread();
        this.runningThread.run();
        super.open();
    }

    public void close() {
        super.close();
        child.close();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        close();
        open();
    }

    @Override
    public TupleDesc getTupleDesc() {
        return this.child.getTupleDesc();
    }

    @Override
    protected Tuple fetchNext() throws DbException, TransactionAbortedException {
        try {
            // wait until the working thread terminate and return an empty tuple set
            runningThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[]{this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        if(this.child != children[0]) {
            this.child = children[0];
        }
    }
}
