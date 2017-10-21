package org.jocean.xharbor.reactor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jocean.idiom.DisposableWrapper;
import org.jocean.idiom.Ordered;
import org.jocean.xharbor.api.TradeReactor;
import org.jocean.xharbor.api.TradeReactor.InOut;
import org.jocean.xharbor.api.TradeReactor.ReactContext;
import org.junit.Test;

import io.netty.handler.codec.http.HttpObject;
import rx.Observable;
import rx.Single;
import rx.functions.Func2;

public class CompositeReactorTestCase {
    
    class OrderedTradeReactor implements TradeReactor, Ordered {

        OrderedTradeReactor(final int ordinal, final Func2<ReactContext, InOut, Single<? extends InOut>> doReact) {
            this._ordinal = ordinal;
            this._doReact = doReact;
        }
        @Override
        public int ordinal() {
            return this._ordinal;
        }
        @Override
        public Single<? extends InOut> react(final ReactContext ctx, final InOut io) {
            return this._doReact.call(ctx, io);
        }
        
        private final int _ordinal;
        private final Func2<ReactContext, InOut, Single<? extends InOut>> _doReact;
    }

    @Test
    public final void testCompositeReactor() {
        final CompositeReactor cr = new CompositeReactor();
        final AtomicBoolean tr1Reacted = new AtomicBoolean(false);
        final AtomicBoolean tr2Reacted = new AtomicBoolean(false);
        
        cr.addReactor(new OrderedTradeReactor(1, new Func2<ReactContext, InOut, Single<? extends InOut>>() {
            @Override
            public Single<? extends InOut> call(ReactContext ctx, InOut t2) {
                tr1Reacted.set(true);
                assertTrue(tr2Reacted.get());
                return Single.just(null);
            }}));
        
        cr.addReactor(new OrderedTradeReactor(2, new Func2<ReactContext, InOut, Single<? extends InOut>>() {
            @Override
            public Single<? extends InOut> call(ReactContext ctx, InOut t2) {
                tr2Reacted.set(true);
                assertFalse(tr1Reacted.get());
                return Single.just(null);
            }}));
        
        final InOut io =
        cr.react(null, new InOut() {
            @Override
            public Observable<? extends DisposableWrapper<HttpObject>> inbound() {
                return null;
            }
            @Override
            public Observable<? extends HttpObject> outbound() {
                return null;
            }})
        .toBlocking().value();
        
        assertTrue(tr1Reacted.get());
        assertTrue(tr2Reacted.get());
        assertNull(io);
    }
}
