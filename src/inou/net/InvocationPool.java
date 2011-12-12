package inou.net;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Thread pool executor class.
 * I should use java.util.concurrent package in Java 2 SE 5.
 */
public class InvocationPool {

	private int maxJob   = 4;
	private int maxWorker = 4;
	private String name;

	private LinkedList jobQueue = new LinkedList();
	private Worker[] workers;
	private Logger monitor = Logger.getLogger( this.getClass() );

	public InvocationPool(int maxWorker,int maxJob,String name) {
		this.maxJob = maxJob;
		this.maxWorker = maxWorker;
		this.name = name;
		init();
	}

	public void init() {
		if (workers!=null) return;
		monitor.debug("IPool:  Initailize workers.");
		workers = new Worker[maxWorker];
		for(int i=0;i<maxWorker;i++) {
			workers[i] = new Worker(name+":"+i);
		}
	}

	public void invokes(Runnable job) {
		if (workers == null) {
			throw new RuntimeException("IPool Bug : worker is not ready!");
		}
		monitor.debug("IPool:  Job coming : "+job.toString());
		while(true) {
			synchronized(jobQueue) {
				if (jobQueue.size() < maxJob) {
					jobQueue.add(job);
					jobQueue.notifyAll();
					break;
				}
				try {
					monitor.debug("IPool:  waiting for queue... ");
					jobQueue.wait();
				} catch (InterruptedException e) {
					monitor.debug(e.getMessage(),e);
				}
			}
		}
	}
    
    public void finalize() throws Throwable {
        dispose();
		super.finalize();
    }

	public void dispose() {
		for(int i=0;i<workers.length;i++) {
			workers[i].finish();
		}
		synchronized(jobQueue) {
			jobQueue.notifyAll();
		}
		workers = null;
	}

	private class Worker {

		private boolean[] finishFlag = {false};

		private Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						workloop();
					} catch (Throwable t) {
						monitor.warn(t.getMessage(),t);
					} finally {
						monitor.debug("InvocationPool: thread finished.");
					}
				}
			});

		Worker(String name) {
            thread.setName(name);
			thread.start();
		}

		void finish() {
			synchronized(finishFlag) {
				finishFlag[0] = true;
			}
		}

		private void workloop() {
			while(true) {
				Runnable job = null;
				synchronized(jobQueue) {
					if (jobQueue.size() > 0) {
						job = (Runnable)jobQueue.removeFirst();
						jobQueue.notifyAll();
					} else {
						try {
							jobQueue.wait();
						} catch (InterruptedException e) {
							monitor.warn(e.getMessage(),e);
						}
					}
				}
				if (job != null) {
					job.run();
				}
				synchronized(finishFlag) {
					if (finishFlag[0]) {
						break;
					}
				}
			}
		}
		
	}

	//=================================================================
	// BENCH MARK TEST
	//=================================================================

	public static void main(String[] args) {
		int num = 1000;
		long threadTime = 0,poolTime = 0,single = 0;
		int maxjob = 100;
		int maxworker = 10;
        Logger mon = Logger.getLogger(InvocationPool.class);
		InvocationPool pool = new InvocationPool(maxworker,maxjob,"test");
		for(int i=0;i<3;i++) {
			System.out.println("Thread");
			threadTime += testThread(num,mon);
			System.out.println(""+threadTime);
			System.out.println("Pool");
			poolTime += testPool(num,pool,mon);
			System.out.println(""+poolTime);
			System.out.println("Single");
			single += testSingle(num,mon);
			System.out.println(""+single);
			System.out.println("##");
		}
		System.out.println("-----------------------");
		System.out.println("Thread test");
		System.out.println(""+threadTime);
		System.out.println("Pool   test");
		System.out.println(""+poolTime);
		System.out.println("Single test");
		System.out.println(""+single);
		pool.dispose();
	}

	private static long testPool(int num,InvocationPool pool,Logger mon) {
		LinkedList results = new LinkedList();
		Runnable[] jobs = makeJobs(results,num);
		long start = System.currentTimeMillis();
		for(int i=0;i<jobs.length;i++) {
			pool.invokes(jobs[i]);
		}
		wait(results,num,mon);
		return System.currentTimeMillis() - start;
	}

	private static long testThread(int num,Logger mon) {
		LinkedList results = new LinkedList();
		Runnable[] jobs = makeJobs(results,num);
		long start = System.currentTimeMillis();
		for(int i=0;i<jobs.length;i++) {
			new Thread(jobs[i]).start();
		}
		wait(results,num,mon);
		return System.currentTimeMillis() - start;
	}

	private static long testSingle(int num,Logger mon) {
		LinkedList results = new LinkedList();
		Runnable[] jobs = makeJobs(results,num);
		long start = System.currentTimeMillis();
		for(int i=0;i<jobs.length;i++) {
			jobs[i].run();
		}
		wait(results,num,mon);
		return System.currentTimeMillis() - start;
	}

	private static void wait(List results,int num,Logger mon) {
		while(true) {
			synchronized(results) {
				if (results.size() == num) {
					break;
				}
				try {
					results.wait();
				} catch (InterruptedException e) {
					mon.warn(e.getMessage(),e);
				}
			}
		}
	}

	private static Runnable[] makeJobs(List rs,int jobs) {
		final List results = rs;
		Runnable[] ret = new Runnable[jobs];
		for(int i=0;i<jobs;i++) {
			final int aaa = i;
			ret[i] = new Runnable() {
					public void run() {
						int timer = (int)(Math.random()*100);
						for(int i=0;i<timer;i++) {
							Date d = new Date(System.currentTimeMillis()-(long)(Math.random()*1000000));
							String a = d.toString();
							a.length();
						}
						synchronized(results) {
							results.add(new Integer(aaa));
							results.notify();
						}
					}
				};
		}
		return ret;
	}

}
