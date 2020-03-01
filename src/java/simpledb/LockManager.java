package simpledb;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {
    private ConcurrentHashMap<PageId, List<LockProfile>> lockprofileMap;
    private ConcurrentHashMap<TransactionId, PageId> waitinglist;
    public LockManager() {
        lockprofileMap = new ConcurrentHashMap<>();
        waitinglist = new ConcurrentHashMap<>();
    }

    public synchronized boolean grantRlock(TransactionId tid, PageId pid) {
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list != null && list.size() != 0) {
            if (list.size() != 1)  {
                for (LockProfile lp : list) {
                    if (lp.getPerm() == Permissions.READ_WRITE) {
                        return lp.getTid().equals(tid) || wait(tid, pid);
                    } else if (lp.getTid().equals(tid)) {
                        return true;
                    }
                }
                return lock(pid, tid, Permissions.READ_ONLY);
            }
            else {
                LockProfile lp = list.iterator().next();
                if (lp.getTid().equals(tid)) {
                    if(lp.getPerm()== Permissions.READ_ONLY){
                        return true;
                    }
                    else{
                        return lock(pid, tid, Permissions.READ_ONLY);
                    }
                } else {
                    if (lp.getPerm()== Permissions.READ_ONLY){
                        return lock(pid, tid, Permissions.READ_ONLY);
                    }
                    else{
                        return wait(tid,pid);
                    }
                }
            }
        }
        else {
            return lock(pid, tid, Permissions.READ_ONLY);
        }
    }

    public synchronized boolean grantWlock(TransactionId tid, PageId pid) {
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list != null && list.size() != 0) {
            if (list.size() == 1) {
                LockProfile lp = list.iterator().next();
                if(lp.getTid().equals(tid)){
                    return lp.getPerm()== Permissions.READ_ONLY||lock(pid,tid,Permissions.READ_WRITE);
                }
                else{
                    return wait(tid,pid);
                }
            }
            else {
                if (list.size() == 2) {
                    for (LockProfile lp : list) {
                        if (lp.getTid().equals(tid) && lp.getPerm() == Permissions.READ_WRITE) {
                            return true;
                        }
                    }
                }
                return wait(tid, pid);
            }
        } else {
            return lock(pid, tid, Permissions.READ_WRITE);
        }
    }

    private synchronized boolean lock(PageId pid, TransactionId tid, Permissions perm) {
        LockProfile lock = new LockProfile(tid, perm);
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(lock);
        lockprofileMap.put(pid, list);
        waitinglist.remove(tid);
        return true;
    }


    private synchronized boolean wait(TransactionId tid, PageId pid) {
        waitinglist.put(tid, pid);
        return false;
    }

    public synchronized boolean unlock(TransactionId tid, PageId pid) {
        if (lockprofileMap.get(pid) == null || lockprofileMap.get(pid).size() == 0) return false;
        ArrayList<LockProfile> list =new ArrayList<>();
        list.addAll(lockprofileMap.get(pid));
        LockProfile lp = getLockProfile(tid, pid);
        if (lp == null) return false;
        list.remove(lp);
        lockprofileMap.put(pid, list);
        return true;
    }

    public synchronized void releaseTransactionLocks(TransactionId tid) {
        List<PageId> toRelease = getAllLocksByTid(tid);
        for (PageId pid : toRelease) {
            unlock(tid, pid);
        }
    }

    public synchronized boolean deadlockOccurred(TransactionId tid, PageId pid) {
        List<LockProfile> holdlocks = lockprofileMap.get(pid);
        if (holdlocks == null || holdlocks.size() == 0) {
            return false;
        }
        List<PageId> pids = getAllLocksByTid(tid);
        for (LockProfile Lp : holdlocks) {
            TransactionId holder = Lp.getTid();
            if (!holder.equals(tid)) {
                boolean isWaiting = isWaitingResources(holder, pids, tid);
                if (isWaiting) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     check whether lock impacts the process.
     */
    private synchronized boolean isWaitingResources(TransactionId tid, List<PageId> pids, TransactionId toRemove) {
        PageId waitingPage = waitinglist.get(tid);
        if (waitingPage == null) {
            return false;
        }

        for (PageId pid : pids) {
            if (pid.equals(waitingPage)) {
                return true;
            }
        }

        List<LockProfile> holders = lockprofileMap.get(waitingPage);
        if (holders == null || holders.size() == 0) return false;
        for (LockProfile lp : holders) {
            TransactionId holder = lp.getTid();
            if (!holder.equals(toRemove)) {
                boolean isWaiting = isWaitingResources(holder, pids, toRemove);
                if (isWaiting) return true;
            }
        }
        return false;
    }

    /**
        get the all Lockprofile by pid, and return the lockprofile.
        if transactionId do not contain lock page, it will skip the lock.
        else return null if cannot find lock page does not exist.
     */
    public synchronized LockProfile getLockProfile(TransactionId tid, PageId pid) {
        if (lockprofileMap.get(pid) == null || lockprofileMap.get(pid).size() == 0) {
            return null;
        }
        ArrayList<LockProfile> list=new ArrayList<>();
        list.addAll(lockprofileMap.get(pid));
        for (LockProfile lockprofile : list) {
            if (lockprofile.getTid().equals(tid)) {
                return lockprofile;
            }
        }
        return null;
    }

    /**
     * get all locks of tid
     */
    private synchronized List<PageId> getAllLocksByTid(TransactionId tid) {
        ArrayList<PageId> pids = new ArrayList<>();
        for (PageId pid : lockprofileMap.keySet()){
            for(LockProfile lp: lockprofileMap.get(pid)){
                if (lp.getTid().equals(tid)) {
                    pids.add(pid);
                }
            }
        }
        return pids;
    }
}
 class LockProfile {
    private TransactionId tid;
    private Permissions perm;
    public LockProfile(TransactionId tid, Permissions perm) {
        this.tid = tid;
        this.perm = perm;
    }

    public TransactionId getTid() {
        return tid;
    }
    public Permissions getPerm() {
        return perm;
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LockProfile LockProfile = (LockProfile) o;
        return tid.equals(LockProfile.tid) && perm.equals(LockProfile.perm);
    }
}