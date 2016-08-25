package org.wildfly.extras.creaper.commands.infinispan.cache;

import org.wildfly.extras.creaper.core.online.OnlineCommand;
import org.wildfly.extras.creaper.core.online.OnlineCommandContext;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;

abstract class AbstractAddCache implements OnlineCommand {

    private final CacheType cacheType;
    private final String cacheContainer;
    private final String name;

    // cache properties common to all cache types
    private final String jndiName;
    private final String module;
    private final String start;
    private final Boolean statisticsEnabled;

    protected final Address address;

    @Override
    public final void apply(OnlineCommandContext ctx) throws Exception {
        Values values = Values.empty()
                .andOptional("jndi-name", jndiName)
                .andOptional("module", module)
                .andOptional("start", start)
                .andOptional("statistics-enabled", statisticsEnabled);
        values = addValuesSpecificForCacheType(values);

        Operations ops = new Operations(ctx.client);
        ops.add(address, values);
    }

    abstract Values addValuesSpecificForCacheType(Values generalCacheValues);

    AbstractAddCache(Builder builder, CacheType cacheType) {
        if (builder.cacheContainer == null) {
            throw new NullPointerException("Cache container is required");
        }
        this.cacheContainer = builder.cacheContainer;
        this.cacheType = cacheType;
        this.name = builder.name;
        this.jndiName = builder.jndiName;
        this.module = builder.module;
        this.start = builder.start;
        this.statisticsEnabled = builder.statisticsEnabled;
        this.address = Address.subsystem("infinispan")
                .and("cache-container", cacheContainer)
                .and(cacheType.getType(), name);
    }

    public abstract static class Builder<THIS extends Builder> {

        protected String name;
        protected String cacheContainer;
        protected String jndiName;
        protected String module;
        protected String start;
        protected Boolean statisticsEnabled;

        protected Builder(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name of the cache must be specified");
            }
            this.name = name;
        }

        public final THIS cacheContainer(String cacheContainer) {
            this.cacheContainer = cacheContainer;
            return (THIS) this;
        }

        public final THIS jndiName(String jndiName) {
            this.jndiName = jndiName;
            return (THIS) this;
        }

        public final THIS module(String module) {
            this.module = module;
            return (THIS) this;
        }

        public final THIS start(String start) {
            this.start = start;
            return (THIS) this;
        }

        public final THIS statisticsEnabled(Boolean statisticsEnabled) {
            this.statisticsEnabled = statisticsEnabled;
            return (THIS) this;
        }

    }
}
