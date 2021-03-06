package org.wildfly.extras.creaper.commands.messaging;

import org.wildfly.extras.creaper.commands.foundation.offline.xml.GroovyXmlTransform;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.Subtree;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.offline.OfflineCommand;
import org.wildfly.extras.creaper.core.offline.OfflineCommandContext;
import org.wildfly.extras.creaper.core.online.OnlineCommand;
import org.wildfly.extras.creaper.core.online.OnlineCommandContext;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Batch;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;

import java.io.IOException;
import java.util.List;

/**
 * Creates new messaging queue.
 */
public final class AddQueue implements OnlineCommand, OfflineCommand {
    private final String name;
    private final String serverName;
    private boolean durable;
    private List<String> jndiEntries;
    private String selector;
    private final boolean replaceExisting;

    private AddQueue(Builder builder) {
        this.name = builder.name;
        this.serverName = builder.serverName;
        this.durable = builder.durable;
        this.jndiEntries = builder.jndiEntries;
        this.selector = builder.selector;
        this.replaceExisting = builder.replaceExisting;
    }

    @Override
    public void apply(OnlineCommandContext ctx) throws IOException, CommandFailedException {
        Operations ops = new Operations(ctx.client);

        Address queueAddress = MessagingUtils.address(ctx.client, serverName).and("jms-queue", name);

        if (replaceExisting) {
            try {
                ops.removeIfExists(queueAddress);
            } catch (OperationException e) {
                throw new CommandFailedException("Failed to remove existing queue " + name, e);
            }
        }

        Values values = Values.empty()
                .andOptional("durable", durable)
                .andOptional("selector", selector)
                .andList(String.class, "entries", jndiEntries);

        Batch batch = new Batch();
        batch.add(queueAddress, values);

        ops.batch(batch);
    }

    @Override
    public void apply(OfflineCommandContext ctx) throws CommandFailedException, IOException {
        if (!MessagingUtils.DEFAULT_SERVER_NAME.equals(serverName)) {
            throw new CommandFailedException("Non-default messaging server name not yet implemented in offline mode");
        }

        GroovyXmlTransform transform = GroovyXmlTransform.of(AddQueue.class)
                .subtree("messagingHornetq", Subtree.subsystem("messaging"))
                .subtree("messagingActivemq", Subtree.subsystem("messaging-activemq"))
                .parameter("name", name)
                .parameter("durable", durable)
                .parameter("selector", selector)
                .parameter("entries", jndiEntries)
                .parameter("entriesString", MessagingUtils.getStringOfEntries(jndiEntries))
                .parameter("replaceExisting", replaceExisting)
                .build();

        ctx.client.apply(transform);
    }

    @Override
    public String toString() {
        return "AddQueue " + name;
    }

    public static final class Builder {
        private final String name;
        private final String serverName;
        private boolean durable;
        private List<String> jndiEntries;
        private String selector;
        private boolean replaceExisting;

        /**
         * Adds a queue to the default messaging server.
         * @param name name of the queue
         */
        public Builder(String name) {
            this(name, MessagingUtils.DEFAULT_SERVER_NAME);
        }

        /**
         * Adds a queue to the specified messaging server. <b>NOT YET IMPLEMENTED FOR OFFLINE!</b>
         * @param name name of the queue
         * @param serverName name of the messaging server
         */
        public Builder(String name, String serverName) {
            if (name == null) {
                throw new IllegalArgumentException("Queue name must be specified as non null value");
            }
            if (serverName == null) {
                throw new IllegalArgumentException("Messaging server name must be specified as non null value");
            }

            this.name = name;
            this.serverName = serverName;
        }

        /**
         * Defines the queue durability
         */
        public Builder durable(boolean durable) {
            this.durable = durable;
            return this;
        }

        /**
         * Defines the list of jndi entries to which this queue is bound to.
         */
        public Builder jndiEntries(List<String> jndiEntries) {
            this.jndiEntries = jndiEntries;
            return this;
        }

        /**
         * Defines the queue selector
         */
        public Builder selector(String selector) {
            this.selector = selector;
            return this;
        }

        /**
         * Specify whether to replace the existing queue based on its name. By
         * default existing queue is not replaced and exception is thrown.
         */
        public Builder replaceExisting() {
            this.replaceExisting = true;
            return this;
        }

        public AddQueue build() {
            check();
            return new AddQueue(this);
        }

        private void check() {
            if (jndiEntries == null || jndiEntries.isEmpty()) {
                throw new IllegalArgumentException("At least one jndi entry needs to be specified for queue");
            }
        }
    }
}
