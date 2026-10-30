package tech.sadnes.client.event;

public final class NoLambdaFactoryException extends RuntimeException {
    public NoLambdaFactoryException(Class<?> clazz) {
        super("No registered lambda listener for '" + clazz.getName() + "'.");
    }
}
