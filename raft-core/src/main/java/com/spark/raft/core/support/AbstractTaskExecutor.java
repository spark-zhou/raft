package com.spark.raft.core.support;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class AbstractTaskExecutor implements TaskExecutor {

    @Override
    public void submit(@Nonnull Runnable task, @Nonnull FutureCallback<Object> callback) {
        Preconditions.checkNotNull(task);
        Preconditions.checkNotNull(callback);
        submit(task, Collections.singletonList(callback));
    }
}
