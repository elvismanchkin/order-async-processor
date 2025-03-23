package dev.demo.order.async.processor;

import io.micrometer.context.ContextSnapshot;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

public class ContextSnapshotSubscriber<T> implements CoreSubscriber<T> {
    private final CoreSubscriber<T> delegate;
    private final ContextSnapshot contextSnapshot;
    private final Context context;

    public ContextSnapshotSubscriber(CoreSubscriber<T> delegate,
                                     ContextSnapshot contextSnapshot,
                                     Context context) {
        this.delegate = delegate;
        this.contextSnapshot = contextSnapshot;
        this.context = context;
    }

    @Override
    public void onSubscribe(Subscription s) {
        contextSnapshot.wrap(() -> delegate.onSubscribe(s)).run();
    }

    @Override
    public void onNext(T t) {
        contextSnapshot.wrap(() -> delegate.onNext(t)).run();
    }

    @Override
    public void onError(Throwable t) {
        contextSnapshot.wrap(() -> delegate.onError(t)).run();
    }

    @Override
    public void onComplete() {
        contextSnapshot.wrap(delegate::onComplete).run();
    }

    @Override
    public Context currentContext() {
        return context.putAll(delegate.currentContext());
    }
}