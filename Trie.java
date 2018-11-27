import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Trie {
    static HashMap<Character, TrieNode> myStartingLetters;

    public Trie() {
        myStartingLetters = new HashMap<>();
    }

    public void addWord(String word, String meaning, Map<String, Object> info) {
        if (word.length() == 0) {
            return;
        } else if (!myStartingLetters.containsKey(word.charAt(0))) {
            myStartingLetters.put(word.charAt(0), new TrieNode(word.charAt(0)));
            myStartingLetters.get(word.charAt(0)).addHelper(word.substring(1), meaning, info);
        } else {
            myStartingLetters.get(word.charAt(0)).addHelper(word.substring(1), meaning, info);
        }
    }

    public static List<Map<String, Object>> wordsContainsExactly(String word) {
        if (!myStartingLetters.containsKey(word.charAt(0))) {
            return null;
        }
        TrieNode rtn = myStartingLetters.get(word.charAt(0)).traverse(word.substring(1));
        return rtn.locationInfo;
    }

    public static List wordsWithPrefix(String word) {
        if (!myStartingLetters.containsKey(word.charAt(0))) {
            return null;
        }
        TrieNode curr = myStartingLetters.get(word.charAt(0)).traverse(word.substring(1));
        if (curr == null) {
            return null;
        }
        List<String> subordinates = curr.getAllSubordinateWords(new ArrayList<String>());
        for (String s : subordinates) {
            s = word.concat(s);
        }
        return subordinates;
    }
}
