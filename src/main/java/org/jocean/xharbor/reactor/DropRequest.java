package org.jocean.xharbor.reactor;

import org.jocean.http.CloseException;
import org.jocean.http.HttpSlice;
import org.jocean.http.HttpSliceUtil;
import org.jocean.idiom.DisposableWrapper;
import org.jocean.xharbor.api.TradeReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import rx.Observable;
import rx.Single;

public class DropRequest implements TradeReactor {

    private static final Logger LOG = LoggerFactory
            .getLogger(DropRequest.class);

    public DropRequest(
            final MatchRule matcher,
            final boolean enableLog) {
        this._matcher = matcher;
        this._log = enableLog;
    }

    @Override
    public Single<? extends InOut> react(final ReactContext ctx, final InOut io) {
        if (null != io.outbound()) {
            return Single.<InOut>just(null);
        }
        return io.inbound().compose(HttpSliceUtil.<HttpRequest>extractHttpMessage()).map(req -> {
            if (null == req) {
                LOG.warn("request is null, ignore trade {}", ctx.trade());
                return null;
            } else {
                if (_matcher.match(req)) {
                    return io4Drop(ctx, io, req);
                } else {
                    // not handle this trade
                    return null;
                }
            }
        }).toSingle();
    }

    private InOut io4Drop(final ReactContext ctx, final InOut originalio,
            final HttpRequest originalreq) {
        return new InOut() {
            @Override
            public Observable<? extends HttpSlice> inbound() {
                return originalio.inbound();
            }
            @Override
            public Observable<? extends Object> outbound() {
                return Observable.<DisposableWrapper<HttpObject>>error(new CloseException())
                    .doOnError(e -> {
                            if (e instanceof CloseException && _log) {
                                LOG.info("Drop request directly:\nREQ\n[{}]", originalreq);
                            }
                        })
                    ;
            }};
    }

    private final MatchRule _matcher;
    private final boolean _log;
}
