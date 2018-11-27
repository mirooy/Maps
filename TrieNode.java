import java.util.*;


public class TrieNode {
    HashMap<Character, TrieNode> myNextLetters;

    // Leave this false if this TrieNode is not the end of a complete word.

    boolean isWord;
    String word;
    char containedLetter;
    LinkedList<Map<String, Object>> locationInfo;

    public TrieNode(Character letter) {
        this.containedLetter = letter;
        this.myNextLetters = new HashMap<>();
        this.locationInfo = new LinkedList<>();
    }

    public void addHelper(String x, String meaning, Map<String, Object> info) {
        if (x.length() == 0) {
            return;
        } else if (x.length() == 1) {
            if (!myNextLetters.containsKey(x.charAt(0))) {
                myNextLetters.put(x.charAt(0), new TrieNode(x.charAt(0)));
            }
            myNextLetters.get(x.charAt(0)).isWord = true;
            myNextLetters.get(x.charAt(0)).word = meaning;
            myNextLetters.get(x.charAt(0)).locationInfo.add(info);
        } else if (!myNextLetters.containsKey(x.charAt(0))) {
            myNextLetters.put(x.charAt(0), new TrieNode(x.charAt(0)));
            myNextLetters.get(x.charAt(0)).addHelper(x.substring(1), meaning, info);
        } else {
            myNextLetters.get(x.charAt(0)).addHelper(x.substring(1), meaning, info);
        }
    }

    public TrieNode traverse(String x) {
        if (myNextLetters.containsKey(x.charAt(0)) && x.length() == 1) {
            return myNextLetters.get(x.charAt(0));
        } else if (!myNextLetters.containsKey(x.charAt(0))) {
            return null;
        } else {
            return myNextLetters.get(x.charAt(0)).traverse(x.substring(1));
        }
    }

    public List<String> getAllSubordinateWords(List<String> rtn) {

        if (this.myNextLetters.isEmpty()) {
            rtn.add(this.word);
            return rtn;
        } else if (this.isWord) {
            rtn.add(this.word);
            for (Character c : this.myNextLetters.keySet()) {
                myNextLetters.get(c).getAllSubordinateWords(rtn);
            }
        } else {
            for (Character c : this.myNextLetters.keySet()) {
                myNextLetters.get(c).getAllSubordinateWords(rtn);
            }
        }
        return rtn;
    }
}
