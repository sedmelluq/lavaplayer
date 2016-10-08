package com.sedmelluq.discord.lavaplayer.tools;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;

/**
 * Wrapper for executor services which ensures that tasks with the same key are processed in order.
 */
public class OrderedExecutor {
  private final ExecutorService delegateService;
  private final ConcurrentMap<Object, BlockingQueue<Runnable>> states;

  /**
   * @param delegateService Executor service where to delegate the actual execution to
   */
  public OrderedExecutor(ExecutorService delegateService) {
    this.delegateService = delegateService;
    this.states = new ConcurrentHashMap<>();
  }

  /**
   * @param orderingKey Key for the ordering channel
   * @param runnable Runnable to submit to the executor service
   * @return Future for the task
   */
  public Future<Void> submit(Object orderingKey, Runnable runnable) {
    RunnableFuture<Void> runnableFuture = newTaskFor(runnable, null);
    queueOrSubmit(new ChannelRunnable(orderingKey), runnableFuture);
    return runnableFuture;
  }

  /**
   * @param orderingKey Key for the ordering channel
   * @param callable Callable to submit to the executor service
   * @return Future for the task
   */
  public <T> Future<T> submit(Object orderingKey, Callable<T> callable) {
    RunnableFuture<T> runnableFuture = newTaskFor(callable);
    queueOrSubmit(new ChannelRunnable(orderingKey), runnableFuture);
    return runnableFuture;
  }

  private void queueOrSubmit(ChannelRunnable runnable, Runnable delegate) {
    BlockingQueue<Runnable> newQueue = new LinkedBlockingQueue<>();
    newQueue.add(delegate);

    BlockingQueue<Runnable> existing = states.putIfAbsent(runnable.key, newQueue);

    if (existing != null) {
      existing.add(delegate);

      if (states.putIfAbsent(runnable.key, existing) == null) {
        delegateService.execute(new ChannelRunnable(runnable.key));
      }
    } else {
      delegateService.execute(runnable);
    }
  }

  private <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new FutureTask<>(runnable, value);
  }

  private <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    return new FutureTask<>(callable);
  }

  private class ChannelRunnable implements Runnable {
    private final Object key;

    private ChannelRunnable(Object key) {
      this.key = key;
    }

    @Override
    public void run() {
      BlockingQueue<Runnable> queue = states.get(key);

      if (queue != null) {
        executeQueue(queue);
      }
    }

    private void executeQueue(BlockingQueue<Runnable> queue) {
      Runnable next;

      while ((next = queue.poll()) != null) {
        boolean finished = false;

        try {
          next.run();
          finished = true;
        } finally {
          if (!finished) {
            delegateService.execute(new ChannelRunnable(key));
          }
        }
      }

      states.remove(key, queue);
    }
  }
}
