package io.dico.dicore.command.annotation;

public class CommandAnnotationUtils {

    /**
     * Get the short description from a {@link Desc} annotation.
     * If {@link Desc#shortVersion()} is given, returns that.
     * Otherwise, returns the first element of {@link Desc#value()}
     * If neither is available, returns null.
     *
     * @param desc the annotation
     * @return the short description
     */
    public static String getShortDescription(Desc desc) {
        String descString;
        if (desc == null) {
            descString = null;
        } else if (!desc.shortVersion().isEmpty()) {
            descString = desc.shortVersion();
        } else if (desc.value().length > 0) {
            descString = desc.value()[0];
            if (desc.value().length > 1) {
                //System.out.println("[Command Warning] Multiline descriptions not supported here. Keep it short for: " + targetIdentifier);
            }
            if (descString != null && descString.isEmpty()) {
                descString = null;
            }
        } else {
            descString = null;
        }

        return descString;
    }

}
