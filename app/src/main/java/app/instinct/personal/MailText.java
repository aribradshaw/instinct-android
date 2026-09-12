package app.instinct.personal;

final class MailText {
    static String conversational(String raw) {
        String value = raw.replace("\r\n", "\n").trim();
        // Retain the complete original separately; only hide quoted email history in the chat view.
        value = value.replaceFirst("(?s)\nOn [^\n]*(?:\n[^\n]*){0,3}wrote:\\s*\n>.*$", "");
        value = value.replaceFirst("(?s)\n_{5,}\nFrom:.*$", "");
        value = value.replaceFirst("(?s)\n-- \n.*$", "");
        return value.trim();
    }
    static boolean exactAddress(String actual, String expected) { return actual != null && actual.equalsIgnoreCase(expected); }
}
