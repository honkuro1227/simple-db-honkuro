package simpledb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {

    private Map<PageId, List<LockProfile>> lockprofileMap;


    private Map<TransactionId, PageId> waitingInfo;
    public LockManager() {

        lockprofileMap = new ConcurrentHashMap<>();
        waitingInfo = new ConcurrentHashMap<>();
    }



    public synchronized boolean grantSLock(TransactionId tid, PageId pid) {
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list != null && list.size() != 0) {
            if (list.size() == 1) {
                LockProfile ls = list.iterator().next();
                if (ls.getTid().equals(tid)) {

                    return ls.getPerm() == Permissions.READ_ONLY || lock(pid, tid, Permissions.READ_ONLY);
                } else {

                    return ls.getPerm() == Permissions.READ_ONLY ? lock(pid, tid, Permissions.READ_ONLY) : wait(tid, pid);
                }
            } else {

                for (LockProfile ls : list) {
                    if (ls.getPerm() == Permissions.READ_WRITE) {

                        return ls.getTid().equals(tid) || wait(tid, pid);
                    } else if (ls.getTid().equals(tid)) {
                        return true;
                    }
                }

                return lock(pid, tid, Permissions.READ_ONLY);
            }
        } else {
            return lock(pid, tid, Permissions.READ_ONLY);
        }
    }


    public synchronized boolean grantXLock(TransactionId tid, PageId pid) {
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list != null && list.size() != 0) {
            if (list.size() == 1) {
                LockProfile ls = list.iterator().next();

                return ls.getTid().equals(tid) ? ls.getPerm() == Permissions.READ_WRITE || lock(pid, tid, Permissions.READ_WRITE) : wait(tid, pid);
            } else {

                if (list.size() == 2) {
                    for (LockProfile ls : list) {
                        if (ls.getTid().equals(tid) && ls.getPerm() == Permissions.READ_WRITE) {
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
        LockProfile nls = new LockProfile(tid, perm);
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(nls);
        lockprofileMap.put(pid, list);
        waitingInfo.remove(tid);
        return true;
    }


    private synchronized boolean wait(TransactionId tid, PageId pid) {
        waitingInfo.put(tid, pid);
        return false;
    }



    public synchronized boolean unlock(TransactionId tid, PageId pid) {
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);

        if (list == null || list.size() == 0) return false;
        LockProfile ls = getLockProfile(tid, pid);
        if (ls == null) return false;
        list.remove(ls);
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
        List<LockProfile> holders = lockprofileMap.get(pid);
        if (holders == null || holders.size() == 0) {
            return false;
        }
        List<PageId> pids = getAllLocksByTid(tid);
        for (LockProfile ls : holders) {
            TransactionId holder = ls.getTid();

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
        PageId waitingPage = waitingInfo.get(tid);
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
        for (LockProfile ls : holders) {
            TransactionId holder = ls.getTid();
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
        ArrayList<LockProfile> list = (ArrayList<LockProfile>) lockprofileMap.get(pid);
        if (list == null || list.size() == 0) {
            return null;
        }
        for (LockProfile lockp : list) {
            if (lockp.getTid().equals(tid)) {
                return lockp;
            }
        }
        return null;
    }

    /**
     * get all locks of tid
     */
    private synchronized List<PageId> getAllLocksByTid(TransactionId tid) {
        ArrayList<PageId> pids = new ArrayList<>();
        for (Map.Entry<PageId, List<LockProfile>> entry : lockprofileMap.entrySet()) {
            for (LockProfile ls : entry.getValue()) {
                if (ls.getTid().equals(tid)) {
                    pids.add(entry.getKey());
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LockProfile LockProfile = (LockProfile) o;
        return tid.equals(LockProfile.tid) && perm.equals(LockProfile.perm);
    }


}