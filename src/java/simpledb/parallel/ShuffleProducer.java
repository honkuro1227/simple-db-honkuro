package simpledb.parallel;

import simpledb.*;
import simpledb.OpIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
/**
 * The producer part of the Shuffle Exchange operator.
 * 
 * ShuffleProducer distributes tuples to the workers according to some
 * partition function (provided as a PartitionFunction object during the
 * ShuffleProducer's instantiation).
 * 
 * */
public class ShuffleProducer extends Producer {

    private static final long serialVersionUID = 1L;
    private ParallelOperatorID operatorID;
    private SocketInfo[] workers;
    private PartitionFunction<?,?> pf;
    private OpIterator child;
    private WorkingThread runningThread;
    public String getName() {
        return "shuffle_p";
    }

    public ShuffleProducer(OpIterator child, ParallelOperatorID operatorID,
                           SocketInfo[] workers, PartitionFunction<?, ?> pf) {
        super(operatorID);
        // some code goes here
        this.child=child;
        this.operatorID=operatorID;
        this.workers=workers;
        this.pf=pf;
    }

    public void setPartitionFunction(PartitionFunction<?, ?> pf) {
        // some code goes here
        this.pf=pf;
    }

    public SocketInfo[] getWorkers() {
        // some code goes here
        return this.workers;
    }

    public PartitionFunction<?, ?> getPartitionFunction() {
        // some code goes here
        return this.pf;
    }

    // some code goes here
    class WorkingThread extends Thread {
        public void run() {
            Map<String, IoSession> workerIdToSession = new HashMap<String, IoSession>();
            Map<String, ArrayList<Tuple>> workerIdToBuffer=new HashMap<String, ArrayList<Tuple>>();
            Map<String, Long> workerIdToLastTime =new HashMap<String,Long>();
            // some code goes here
            for(SocketInfo worker: workers){
                workerIdToSession.put(worker.getId(), ParallelUtility.createSession(
                        worker.getAddress(), getThisWorker().minaHandler, -1));
                workerIdToBuffer.put(worker.getId(), new ArrayList<Tuple>());
                workerIdToLastTime.put(worker.getId(), System.currentTimeMillis());
            }
            try {
                while (child.hasNext()) {
                    Tuple tup = child.next();
                    int partition = pf.partition(tup, child.getTupleDesc());
                    SocketInfo consumerWorker = workers[partition];
                    ArrayList<Tuple> buffer = workerIdToBuffer.get(consumerWorker.getId());
                    IoSession session = workerIdToSession.get(consumerWorker.getId());
                    long lastTime = workerIdToLastTime.get(consumerWorker.getId());
                    buffer.add(tup);
                    if (buffer.size() >= TupleBag.MAX_SIZE) {
                        session.write(new TupleBag(
                                operatorID,
                                getThisWorker().workerID,
                                buffer.toArray(new Tuple[] {}),
                                getTupleDesc()));
                        buffer.clear();
                        lastTime = System.currentTimeMillis();
                    }
                    if (buffer.size() >= TupleBag.MIN_SIZE) {
                        long thisTime = System.currentTimeMillis();
                        if (thisTime - lastTime > TupleBag.MAX_MS) {
                            session.write(new TupleBag(
                                    operatorID,
                                    getThisWorker().workerID,
                                    buffer.toArray(new Tuple[] {}),
                                    getTupleDesc()));
                            buffer.clear();
                            lastTime = thisTime;
                        }
                    }
                    workerIdToLastTime.put(consumerWorker.getId(), lastTime);
                }
                for (SocketInfo worker : workers) {
                    ArrayList<Tuple> buffer = workerIdToBuffer.get(worker.getId());
                    IoSession session = workerIdToSession.get(worker.getId());
                    if (buffer.size() > 0) {
                        session.write(new TupleBag(
                                operatorID,
                                getThisWorker().workerID,
                                buffer.toArray(new Tuple[] {}),
                                getTupleDesc()));
                    }
                    session.write(new TupleBag(operatorID,
                            getThisWorker().workerID)).addListener(new IoFutureListener<WriteFuture>(){
                        @Override
                        public void operationComplete(WriteFuture future) {
                            ParallelUtility.closeSession(future.getSession());
                        }});
                }
            }
            catch (DbException e) {
                e.printStackTrace();
            } catch (TransactionAbortedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        super.open();
        child.open();
        runningThread = new WorkingThread();
        System.out.println("Producer open " + this);
        runningThread.run();
    }

    public void close() {
        // some code goes here
        super.close();
        child.close();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
//        throw new UnsupportedOperationException();
        close();
        open();
    }

    @Override
    public TupleDesc getTupleDesc() {
        // some code goes here
        return child.getTupleDesc();
    }

    @Override
    protected Tuple fetchNext() throws DbException, TransactionAbortedException {
        // some code goes here
        try {
            runningThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
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
    }
}
