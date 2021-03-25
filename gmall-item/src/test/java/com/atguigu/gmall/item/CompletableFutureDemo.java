package com.atguigu.gmall.item;

import com.sun.org.apache.regexp.internal.RE;
import io.swagger.models.auth.In;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sound.midi.Soundbank;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * description:
 *
 * @author Ice on 2021/3/24 in 18:27
 */
@SpringBootTest
public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
            @Override
            public Integer get() {
                System.out.println(Thread.currentThread().getName() + "\tcompletableFuture");
//                int i = 1 / 0;
                return 1024;
            }
        }).thenApply(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer o) {
                System.out.println("thenApply方法，上次返回结果：" + o);
                return o * 2;
            }
        }).whenComplete(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer o, Throwable throwable) {
                System.out.println("-------o=" + o);
                System.out.println("-------throwable=" + throwable);
            }
        }).exceptionally(new Function<Throwable, Integer>() {
            @Override
            public Integer apply(Throwable throwable) {
                System.out.println("throwable=" + throwable);
                return 6666;
            }
        });

        System.out.println(future.get());
    }

    @Test
    public void test() {
        List<CompletableFuture<String>> futures = Arrays.asList(
                CompletableFuture.completedFuture("hello"),
                CompletableFuture.completedFuture("world!"),
                CompletableFuture.completedFuture("hello"),
                CompletableFuture.completedFuture("java!"));
        final CompletableFuture<Void> allCompleted = CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
        allCompleted.thenRun(() -> {
            futures.stream().forEach(future -> {
                try {
                    System.out.println("get future at:" + System.currentTimeMillis() + ", result:" + future.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}

