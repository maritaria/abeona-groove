package groove.explore.prettyparse;

import groove.explore.encode.Serialized;

import java.util.Objects;

/**
 * A <code>POptionalExtension</code> is a <code>SerializedParser</code> that consumes a extension <code>SerializedParser</code> element if a prefix literal is found.
 * This allows you to conditionally trigger a SerializedParser to be executed after some other elements.
 *
 * @author Bram Kamies
 * @see SerializedParser
 * @see Serialized
 */
public class POptionalExtension implements SerializedParser {
    private final String trigger;
    private final String argumentName;
    private final SerializedParser extension;

    /**
     * Creates a POptionalExtension
     *
     * @param trigger      - The literal string which will cause the extension to be read
     * @param argumentName - The argument to append the trigger literal to
     * @param extension    - The SerializedParser to invoke if the specified trigger was read from the input
     */
    public POptionalExtension(String trigger, String argumentName, SerializedParser extension) {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(argumentName, "argumentName");
        Objects.requireNonNull(extension, "extension");
        this.trigger = trigger;
        this.argumentName = argumentName;
        this.extension = extension;
    }

    @Override
    public boolean parse(StringConsumer stream, Serialized serialized) {
        if (stream.consumeLiteral(trigger)) {
            serialized.appendArgument(argumentName, trigger);
            return extension.parse(stream, serialized);
        } else {
            return true;
        }
    }

    @Override
    public String describeGrammar() {
        return "[" + this.trigger + this.extension.describeGrammar() + "]";
    }

    @Override
    public String toParsableString(Serialized source) {
        String value = source.getArgument(argumentName);
        if (value.startsWith(trigger)) {
            source.setArgument(argumentName, value.substring(trigger.length()));
            final var extensionString = this.extension.toParsableString(source);
            return trigger + extensionString;
        } else {
            return "";
        }
    }
}
