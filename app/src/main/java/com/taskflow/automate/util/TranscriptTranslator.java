package com.taskflow.automate.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * On-device Hindi/Hinglish to English translator using dictionary and phrase-based approach.
 * Translates meeting/work transcripts so that downstream task extraction operates on English text.
 * No network APIs are used - all translation is rule/dictionary based.
 */
public class TranscriptTranslator {

    private enum Language { ENGLISH, HINDI, HINGLISH }

    private final Map<String, String> phraseMap;
    private final Map<String, String> wordMap;
    private final Map<String, String> devanagariMap;

    public TranscriptTranslator() {
        phraseMap = buildPhraseMap();
        wordMap = buildWordMap();
        devanagariMap = buildDevanagariMap();
    }

    /**
     * Translates a full transcript to English.
     * English sentences are kept as-is. Hindi/Hinglish sentences are translated
     * using phrase-level matching first (longer phrases matched first), then word-level.
     */
    public String translateToEnglish(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return transcript;
        }

        List<String> sentences = splitIntoSentences(transcript);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            Language lang = detectLanguage(sentence);

            if (lang == Language.ENGLISH) {
                result.append(sentence);
            } else {
                result.append(translateSentence(sentence));
            }

            if (i < sentences.size() - 1) {
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Returns true if any sentence in the text is detected as HINDI or HINGLISH.
     */
    public boolean containsNonEnglish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        List<String> sentences = splitIntoSentences(text);
        for (String sentence : sentences) {
            Language lang = detectLanguage(sentence);
            if (lang == Language.HINDI || lang == Language.HINGLISH) {
                return true;
            }
        }
        return false;
    }

    private String translateSentence(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return sentence;
        }

        String working = sentence;

        // First pass: translate Devanagari words/phrases
        for (Map.Entry<String, String> entry : devanagariMap.entrySet()) {
            if (working.contains(entry.getKey())) {
                working = working.replace(entry.getKey(), entry.getValue());
            }
        }

        // Second pass: apply phrase-level translation (longer phrases first)
        List<Map.Entry<String, String>> sortedPhrases = new ArrayList<>(phraseMap.entrySet());
        Collections.sort(sortedPhrases, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return Integer.compare(b.getKey().length(), a.getKey().length());
            }
        });

        String lowerWorking = working.toLowerCase(Locale.US);
        for (Map.Entry<String, String> entry : sortedPhrases) {
            String key = entry.getKey();
            int idx = lowerWorking.indexOf(key);
            if (idx >= 0) {
                working = working.substring(0, idx) + entry.getValue()
                        + working.substring(idx + key.length());
                lowerWorking = working.toLowerCase(Locale.US);
            }
        }

        // Third pass: word-level translation for remaining Hindi/Hinglish words
        String[] words = working.split("\\s+");
        StringBuilder translated = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String lowerWord = word.toLowerCase(Locale.US);
            // Strip trailing punctuation for lookup
            String punctuation = "";
            if (lowerWord.matches(".*[.,;:!?]$")) {
                punctuation = lowerWord.substring(lowerWord.length() - 1);
                lowerWord = lowerWord.substring(0, lowerWord.length() - 1);
                word = word.substring(0, word.length() - 1);
            }

            if (wordMap.containsKey(lowerWord)) {
                translated.append(wordMap.get(lowerWord)).append(punctuation);
            } else {
                // Keep the word as-is (likely English or untranslatable proper noun)
                translated.append(word).append(punctuation);
            }

            if (i < words.length - 1) {
                translated.append(" ");
            }
        }

        return translated.toString().trim();
    }

    /**
     * Detects the primary language of a sentence based on character analysis.
     * Adapted from IntelligentTranscriptAnalyzer.detectLanguage() pattern.
     */
    private Language detectLanguage(String sentence) {
        if (sentence == null || sentence.isEmpty()) {
            return Language.ENGLISH;
        }

        int totalChars = 0;
        int devanagariChars = 0;
        int latinChars = 0;

        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if (Character.isWhitespace(c) || Character.isDigit(c)) {
                continue;
            }
            totalChars++;
            if (c >= '\u0900' && c <= '\u097F') {
                devanagariChars++;
            } else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                latinChars++;
            }
        }

        if (totalChars == 0) {
            return Language.ENGLISH;
        }

        float devanagariRatio = (float) devanagariChars / totalChars;
        float latinRatio = (float) latinChars / totalChars;

        if (devanagariRatio > 0.5f) {
            if (latinRatio > 0.1f) {
                return Language.HINGLISH;
            }
            return Language.HINDI;
        }

        if (latinRatio > 0.5f) {
            if (hasRomanizedHindiMarkers(sentence)) {
                return Language.HINGLISH;
            }
            return Language.ENGLISH;
        }

        if (devanagariRatio > 0.2f && latinRatio > 0.2f) {
            return Language.HINGLISH;
        }

        return Language.ENGLISH;
    }

    private boolean hasRomanizedHindiMarkers(String sentence) {
        String lower = sentence.toLowerCase(Locale.US);
        String[] hindiMarkers = {
            "karo", "karna", "hai", "hoga", "padega", "chahiye",
            "mujhe", "tumhe", "hume", "aapko", "usko",
            "bhej", "dekh", "bata", "kar do", "bhej do",
            "kal", "aaj", "abhi", "tak", "mein",
            "karunga", "karungi", "karenge", "dena", "lena"
        };

        int markerCount = 0;
        for (String marker : hindiMarkers) {
            if (lower.contains(marker)) {
                markerCount++;
            }
        }
        return markerCount >= 2;
    }

    /**
     * Splits text into sentences handling both Latin and Devanagari sentence boundaries.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return sentences;
        }

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("(?<=[.?!\u0964\u0965])\\s*|(?<=\\|)\\s*");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    sentences.add(trimmed);
                }
            }
        }
        return sentences;
    }

    private Map<String, String> buildPhraseMap() {
        Map<String, String> map = new HashMap<>();
        // Meeting/work phrases - Hinglish (romanized Hindi mixed with English)
        map.put("karna hai", "need to do");
        map.put("karna padega", "will have to do");
        map.put("karna hoga", "will need to do");
        map.put("karna chahiye", "should do");
        map.put("kar lena", "do it");
        map.put("kar do", "do it");
        map.put("kar dena", "please do");
        map.put("kar lo", "do it");
        map.put("kar lete hain", "let us do it");
        map.put("bhej do", "send");
        map.put("bhej dena", "please send");
        map.put("bhej de", "send it");
        map.put("kal tak", "by tomorrow");
        map.put("aaj tak", "by today");
        map.put("agle hafte", "next week");
        map.put("agle hafte tak", "by next week");
        map.put("is hafte", "this week");
        map.put("is hafte tak", "by this week");
        map.put("review kar lena", "review it");
        map.put("review karo", "review");
        map.put("review kar do", "please review");
        map.put("check karo", "check it");
        map.put("check kar lo", "check it");
        map.put("check kar lena", "check it");
        map.put("complete karo", "complete it");
        map.put("complete kar do", "please complete");
        map.put("complete kar lena", "complete it");
        map.put("follow up karo", "follow up");
        map.put("follow up kar lena", "follow up on it");
        map.put("plan banao", "make a plan");
        map.put("plan bana lo", "make a plan");
        map.put("status batao", "give status update");
        map.put("status de do", "give status update");
        map.put("confirm karo", "confirm it");
        map.put("confirm kar do", "please confirm");
        map.put("update do", "give update");
        map.put("update de do", "please give update");
        map.put("ready karo", "make ready");
        map.put("ready kar do", "please make ready");
        map.put("ready kar lo", "make it ready");
        map.put("discuss karenge", "will discuss");
        map.put("discuss karo", "discuss");
        map.put("discuss kar lena", "discuss it");
        map.put("meeting mein", "in the meeting");
        map.put("meeting ke baad", "after the meeting");
        map.put("meeting se pehle", "before the meeting");
        map.put("deadline hai", "there is a deadline");
        map.put("deadline de do", "set a deadline");
        map.put("send karo", "send it");
        map.put("send kar do", "please send");
        map.put("send kar dena", "please send");
        map.put("submit karo", "submit");
        map.put("submit kar do", "please submit");
        map.put("fix karo", "fix it");
        map.put("fix kar do", "please fix");
        map.put("handle karo", "handle it");
        map.put("handle kar lo", "handle it");
        map.put("prepare karo", "prepare");
        map.put("prepare kar lo", "prepare it");
        map.put("deliver karna hai", "need to deliver");
        map.put("test karo", "test it");
        map.put("test kar lo", "test it");
        map.put("deploy karo", "deploy it");
        map.put("merge karo", "merge it");
        map.put("create karo", "create it");
        map.put("design karo", "design it");
        map.put("finish karo", "finish it");
        map.put("finish kar do", "please finish");
        map.put("share karo", "share it");
        map.put("share kar do", "please share");
        map.put("schedule karo", "schedule it");
        map.put("assign karo", "assign it");
        map.put("close karo", "close it");
        map.put("resolve karo", "resolve it");
        map.put("ho jaana chahiye", "should be done");
        map.put("ho gaya", "is done");
        map.put("ho jayega", "will be done");
        map.put("dekhna padega", "will need to look at");
        map.put("dekh lena", "look into it");
        map.put("dekh lo", "look into it");
        map.put("samajh lo", "understand it");
        map.put("baat karo", "talk about it");
        map.put("baat kar lena", "talk about it");
        map.put("jald se jald", "as soon as possible");
        map.put("jaldi karo", "do it quickly");
        map.put("jaldi se", "quickly");
        return map;
    }

    private Map<String, String> buildWordMap() {
        Map<String, String> map = new HashMap<>();
        // Common Hindi/Hinglish words used in meetings
        map.put("karo", "do");
        map.put("karna", "to do");
        map.put("karenge", "will do");
        map.put("karunga", "I will do");
        map.put("karungi", "I will do");
        map.put("karoge", "you will do");
        map.put("kiya", "did");
        map.put("karte", "doing");
        map.put("hai", "is");
        map.put("hain", "are");
        map.put("tha", "was");
        map.put("hoga", "will be");
        map.put("hogi", "will be");
        map.put("padega", "will have to");
        map.put("chahiye", "should");
        map.put("mujhe", "I need to");
        map.put("tumhe", "you need to");
        map.put("hume", "we need to");
        map.put("humein", "we need to");
        map.put("aapko", "you need to");
        map.put("usko", "they need to");
        map.put("unko", "they need to");
        map.put("bhejo", "send");
        map.put("bhej", "send");
        map.put("bhejna", "to send");
        map.put("dekho", "look");
        map.put("dekh", "look");
        map.put("dekhna", "to look");
        map.put("batao", "tell");
        map.put("bata", "tell");
        map.put("batana", "to tell");
        map.put("dena", "to give");
        map.put("de", "give");
        map.put("lena", "to take");
        map.put("lo", "take");
        map.put("le", "take");
        map.put("banao", "make");
        map.put("bana", "make");
        map.put("banana", "to make");
        map.put("likho", "write");
        map.put("likh", "write");
        map.put("likhna", "to write");
        map.put("padho", "read");
        map.put("padhna", "to read");
        map.put("kal", "tomorrow");
        map.put("aaj", "today");
        map.put("abhi", "now");
        map.put("tak", "by/until");
        map.put("mein", "in");
        map.put("ke", "of");
        map.put("ka", "of");
        map.put("ki", "of");
        map.put("se", "from");
        map.put("ko", "to");
        map.put("pe", "on");
        map.put("par", "on");
        map.put("aur", "and");
        map.put("ya", "or");
        map.put("lekin", "but");
        map.put("agar", "if");
        map.put("toh", "then");
        map.put("jaldi", "urgently");
        map.put("zaroori", "important");
        map.put("zaruri", "important");
        map.put("zaroor", "definitely");
        map.put("pehle", "before");
        map.put("baad", "after");
        map.put("saath", "with");
        map.put("sab", "all");
        map.put("kuch", "some");
        map.put("naya", "new");
        map.put("purana", "old");
        map.put("accha", "good");
        map.put("theek", "okay");
        map.put("sahi", "correct");
        map.put("galat", "wrong");
        map.put("zyada", "more");
        map.put("kam", "less");
        map.put("bahut", "very");
        map.put("thoda", "a little");
        map.put("bilkul", "absolutely");
        map.put("tum", "you");
        map.put("hum", "we");
        map.put("woh", "they");
        map.put("ye", "this");
        map.put("yeh", "this");
        map.put("wo", "that");
        map.put("kab", "when");
        map.put("kaise", "how");
        map.put("kya", "what");
        map.put("kaun", "who");
        map.put("kahan", "where");
        map.put("kyun", "why");
        map.put("kyunki", "because");
        map.put("isliye", "therefore");
        map.put("phir", "then");
        map.put("document", "document");
        map.put("file", "file");
        map.put("report", "report");
        map.put("email", "email");
        map.put("call", "call");
        map.put("client", "client");
        map.put("team", "team");
        map.put("project", "project");
        map.put("task", "task");
        map.put("priority", "priority");
        map.put("urgent", "urgent");
        map.put("important", "important");
        map.put("toh", "so");
        map.put("fatafat", "quickly");
        map.put("turant", "immediately");
        return map;
    }

    private Map<String, String> buildDevanagariMap() {
        Map<String, String> map = new HashMap<>();
        // Common Hindi action words in Devanagari
        map.put("\u0915\u0930\u094B", "do");                    // करो
        map.put("\u0915\u0930\u0928\u093E", "to do");           // करना
        map.put("\u0915\u0930\u0947\u0902\u0917\u0947", "will do"); // करेंगे
        map.put("\u0915\u0930\u0942\u0901\u0917\u093E", "I will do"); // करूँगा
        map.put("\u092D\u0947\u091C\u094B", "send");            // भेजो
        map.put("\u092D\u0947\u091C\u0928\u093E", "to send");   // भेजना
        map.put("\u092D\u0947\u091C \u0926\u094B", "send it");  // भेज दो
        map.put("\u0926\u0947\u0916\u094B", "look");            // देखो
        map.put("\u0926\u0947\u0916\u0928\u093E", "to look");   // देखना
        map.put("\u0926\u0947\u0916 \u0932\u094B", "look into it"); // देख लो
        map.put("\u092C\u0924\u093E\u0913", "tell");            // बताओ
        map.put("\u0932\u093F\u0916\u094B", "write");           // लिखो
        map.put("\u092A\u0922\u093C\u094B", "read");            // पढ़ो
        map.put("\u092C\u0928\u093E\u0913", "make");            // बनाओ
        map.put("\u0924\u0948\u092F\u093E\u0930", "ready");     // तैयार
        map.put("\u092A\u0942\u0930\u093E", "complete");        // पूरा
        map.put("\u091C\u0932\u094D\u0926\u0940", "urgently");  // जल्दी
        map.put("\u091C\u093C\u0930\u0942\u0930\u0940", "important"); // ज़रूरी
        map.put("\u0915\u0932", "tomorrow");                    // कल
        map.put("\u0906\u091C", "today");                       // आज
        map.put("\u0905\u092D\u0940", "now");                   // अभी
        map.put("\u0924\u0915", "by/until");                    // तक
        map.put("\u092E\u0947\u0902", "in");                    // में
        map.put("\u0914\u0930", "and");                         // और
        map.put("\u0932\u0947\u0915\u093F\u0928", "but");       // लेकिन
        map.put("\u0905\u0917\u0932\u0947 \u0939\u092B\u094D\u0924\u0947", "next week"); // अगले हफ्ते
        map.put("\u0915\u0932 \u0924\u0915", "by tomorrow");    // कल तक
        map.put("\u0906\u091C \u0924\u0915", "by today");       // आज तक
        map.put("\u092E\u0940\u091F\u093F\u0902\u0917", "meeting"); // मीटिंग
        map.put("\u0921\u0947\u0921\u0932\u093E\u0907\u0928", "deadline"); // डेडलाइन
        return map;
    }
}
