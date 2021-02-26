package github.jojo.test.completable;

import github.jojo.remoting.test.completeble.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 22:40
 * @description -------https://mp.weixin.qq.com/s?__biz=MzU0NDA2MjY5Ng==&mid=2247492376&idx=1&sn=06dd0133ff41e858ee74d9d83cc47634&chksm=fb034e9fcc74c789343cd62db8997eabcff489ed3c0a2e9d2fc5a9500a52d96e16be969e421d&mpshare=1&scene=23&srcid=1223yBu45KUqPHJckp2jj55V&sharer_sharetime=1611152660026&sharer_shareid=89618db9620e3252e6bafb1e0940306e#rd-----------
 */
public class testCompleteble {


    @Test
    public void testSync() {
        long start = System.currentTimeMillis();
        List<RemoteLoader> remoteLoaders = Arrays.asList(new CustomerInfoService(), new LearnRecordService());
        //stream流map：接收一个函数作为参数，该函数会被应用到每个元素上，并将其映射成一个新的元素
        List<String> customerDetail = remoteLoaders.stream()
                .map(RemoteLoader::load).collect(toList());
        System.out.println(customerDetail);
        long end = System.currentTimeMillis();
        System.out.println("总共花费时间:" + (end - start));
    }

    /**
     * 使用线程池以及异步调用submit 从Future中阻塞获取结果
     */
    @Test
    public void testFuture() {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<RemoteLoader> remoteLoaders = Arrays.asList(new CustomerInfoService(), new LearnRecordService());
        List<Future<String>> futures = remoteLoaders.stream()
                .map(remoteLoader -> executorService.submit(remoteLoader::load))
                .collect(toList());

        List<String> customerDetail = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(toList());
        System.out.println(customerDetail);
        long end = System.currentTimeMillis();
        System.out.println("总共花费时间:" + (end - start));
    }

    /**
     * JAVA8 并行流
     */
    @Test
    public void testParallelStream() {
        long start = System.currentTimeMillis();
        List<RemoteLoader> remoteLoaders = Arrays.asList(new CustomerInfoService(), new LearnRecordService());
        List<String> customerDetail = remoteLoaders.parallelStream().map(RemoteLoader::load).collect(toList());
        System.out.println(customerDetail);
        long end = System.currentTimeMillis();
        System.out.println("总共花费时间:" + (end - start));
    }

    @Test
    public void testParallelStream2() {
        long start = System.currentTimeMillis();
        List<RemoteLoader> remoteLoaders = Arrays.asList(
                new CustomerInfoService(),
                new LearnRecordService(),
                new LabelService(),
                new OrderService(),
                new WatchRecordService());
        List<String> customerDetail = remoteLoaders.parallelStream().map(RemoteLoader::load).collect(toList());
        System.out.println(customerDetail);
        long end = System.currentTimeMillis();
        System.out.println("总共花费时间:" + (end - start));
    }


    /**
     * CompletableFuture初步使用
     */
    @Test
    public void testCompletableFuture() {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Thread(() -> {
            doSomething();
            future.complete("Finish");          //任务执行完成后 设置返回的结果
        }).start();
        System.out.println(future.join());      //获取任务线程返回的结果
    }

    private void doSomething() {
        System.out.println("doSomething...");
    }

    /**
     * -----------------TEST EXCEPTION-----------
     * 针对任务出现了异常，主线程会无感知，任务线程不会把异常给抛出来；
     * 这会导致主线程会一直等待，通常我们也需要知道出现了什么异常，做出对应的响应；
     * 改进的方式是在任务中try-catch所有的异常，然后调用future.completeExceptionally(e) ，代码如下：
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testCompletableFutureForE() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                doSomethingThrowE();
                future.complete("Finish");
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();
        System.out.println(future.get());
    }

    private void doSomethingThrowE() {
        System.out.println("doSomething...");
        throw new RuntimeException("Test Exception");
    }

    /**
     * 对TEST EXCEPTIONF上面示例 java8进行了封装
     * Java8不仅提供允许任务返回结果的supplyAsync，还提供了没有返回值的runAsync；
     * 让我们可以更加的关注业务的开发，不需要处理异常错误的管理
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testCompletableFuture2() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            doSomethingThrowE();
            return "Finish";
        });
        System.out.println(future.get());
    }

    /**
     * CompletableFuture异常处理
     * 如果说主线程需要关心任务到底发生了什么异常，需要对其作出相应操作，这个时候就需要用到exceptionally
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testCompletableFuture3() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    doSomethingThrowE();
                    return "Finish";
                })
                .exceptionally(throwable -> "Throwable exception message:" + throwable.getMessage());
        System.out.println(future.get());
    }

    /**
     * 使用CompletableFuture来完成我们查询用户详情的API接口
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testCompletableFuture4() throws ExecutionException, InterruptedException {
        long start = System.currentTimeMillis();
        List<RemoteLoader> remoteLoaders = Arrays.asList(
                new CustomerInfoService(),
                new LearnRecordService(),
                new LabelService(),
                new OrderService(),
                new WatchRecordService());
        List<CompletableFuture<String>> completableFutures = remoteLoaders
                .stream()
                .map(loader -> CompletableFuture.supplyAsync(loader::load))
                .collect(toList());

        List<String> customerDetail = completableFutures
                .stream()
                .map(CompletableFuture::join)
                .collect(toList());

        System.out.println(customerDetail);
        long end = System.currentTimeMillis();
        System.out.println("总共花费时间:" + (end - start));
    }

    /**
     * 并行流和CompletableFuture的实现原理。
     * 它们底层使用的线程池的大小都是CPU的核数Runtime.getRuntime().availableProcessors()；
     * 并行流不能自定义线程 CompletableFuture可以使用自定义线程池
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testCompletableFuture5() throws ExecutionException, InterruptedException {
        long start = System.currentTimeMillis();
        List<RemoteLoader> remoteLoaders = Arrays.asList(
                new CustomerInfoService(),
                new LearnRecordService(),
                new LabelService(),
                new OrderService(),
                new WatchRecordService());

        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(remoteLoaders.size(), 50));

        List<CompletableFuture<String>> completableFutures = remoteLoaders
                .stream()
                .map(loader -> CompletableFuture.supplyAsync(loader::load, executorService))
                .collect(toList());

        List<String> customerDetail = completableFutures
                .stream()
                .map(CompletableFuture::join)
                .collect(toList());

        System.out.println(customerDetail);
        long end = System.currentTimeMillis();
        System.out.println("总共花费时间:" + (end - start));
    }

    /**
     * 总结：
     * 并行流和CompletableFuture两者该如何选择
     * 这两者如何选择主要看任务类型，建议
     *
     * 如果你的任务是计算密集型的，并且没有I/O操作的话，那么推荐你选择Stream的并行流，实现简单并行效率也是最高的
     *
     * 如果你的任务是有频繁的I/O或者网络连接等操作，那么推荐使用CompletableFuture，
     * 采用自定义线程池的方式，根据服务器的情况设置线程池的大小，尽可能的让CPU忙碌起来
     */
}
