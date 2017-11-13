package org.idstack.relyingparty;


import com.google.gson.JsonArray;
import org.apache.commons.lang3.tuple.Pair;
import org.idstack.feature.Constant;
import org.idstack.feature.Parser;
import org.idstack.feature.document.Document;
import org.idstack.relyingparty.response.correlation.AttributeScore;
import org.idstack.relyingparty.response.correlation.CorrelationScoreResponse;
import org.idstack.relyingparty.response.correlation.SuperAttribute;

import java.util.*;

/**
 * @author Sachithra Dangalla
 * @date 5/31/2017
 * @since 1.0
 */

@SuppressWarnings("Duplicates")
public class CorrelationScore {

    public static final String EMPTY_VALUE = "-";

    /**
     * @param documentJSONs
     * @return a map of String keys and double[] values (array of name scores of documents)
     */
    public CorrelationScoreResponse getMultipleDocumentScore(JsonArray documentJSONs) {
        int docsLength = documentJSONs.size();
        Document[] docs = new Document[docsLength];
        for (int i = 0; i < docsLength; i++) {
            try {
                Document doc = Parser.parseDocumentJson(documentJSONs.get(i).toString());
                docs[i] = doc;
            } catch (Exception e) {
                return null;
            }
        }
        SuperAttribute name = new SuperAttribute(getNameCorrelationScore(docs));
        SuperAttribute address = new SuperAttribute(getAddressCorrelationScore(docs));
        SuperAttribute dob = new SuperAttribute(getDOBCorrelationScore(docs));
        SuperAttribute gender = new SuperAttribute(getGenderCorrelationScore(docs));
        SuperAttribute nic = new SuperAttribute(getNICCorrelationScore(docs));

        CorrelationScoreResponse cs = new CorrelationScoreResponse(name, address, dob, gender, nic);

        return cs;
    }

    private String getConcatenatedValue(Document doc, Pair<String, String[]> attribute) {
        String name;
        StringJoiner sb = new StringJoiner(" ");
        LinkedHashMap<String, String> content = doc.getContent();
//        Map<String, String> flatAttributeMap = new HashMap<>();

        for (String attributeName : attribute.getRight()) {
            String nameSeg = content.get(attributeName).toLowerCase();
            if (nameSeg != null) {
                sb.add(nameSeg);
            }
        }

        name = sb.toString();
        return name;
    }

    private ArrayList<AttributeScore> getNameCorrelationScore(Document[] docs) {

        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < docs.length; i++) {
            Document doc = docs[i];
            String name = getConcatenatedValue(doc, Constant.Attribute.NAME);
            if (!name.isEmpty()) {
                names.add(name);
            } else {
                names.add(this.EMPTY_VALUE);
            }
        }

        NameScore ns = new NameScore(names);
        ArrayList<AttributeScore> nameScores = ns.getNameScore();

        return nameScores;
    }


    private ArrayList<AttributeScore> getAddressCorrelationScore(Document[] docs) {
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < docs.length; i++) {
            Document doc = docs[i];
            String name = getConcatenatedValue(doc, Constant.Attribute.ADDRESS);
            if (!name.isEmpty()) {
                names.add(name);
            } else {
                names.add(this.EMPTY_VALUE);
            }
        }

        NameScore ns = new NameScore(names);
        ArrayList<AttributeScore> nameScores = ns.getNameScore();

        return nameScores;
    }

    //TODO find date comparison
    private ArrayList<AttributeScore> getDOBCorrelationScore(Document[] docs) {
        int docsLength = docs.length;
        ArrayList<AttributeScore> attrScore = new ArrayList<>();
        String[] nics = new String[docsLength];
        //count candidates with occurrences count
        Map<String, Integer> candidates = new HashMap<>();
        for (int i = 0; i < docs.length; i++) {
            Document doc = docs[i];
            String nic = "";
            //iterate over "dob" attributes
            for (String attributeName : Constant.Attribute.DOB.getRight()) {
                if (doc.getContent().get(attributeName) != null) {
                    nic = doc.getContent().get(attributeName).toLowerCase();
                    Integer count = candidates.get(nic);
                    candidates.put(nic, count != null ? count + 1 : 1);
                    break;
                }
            }
            if (nic.isEmpty()) {
                nic = this.EMPTY_VALUE;
                Integer count = candidates.get(nic);
                candidates.put("", count != null ? count + 1 : 1);
            }

            nics[i] = nic;
        }

        //select most popular nic
        String popular = "";
        if (!candidates.isEmpty()) {
            popular = getPopularString(candidates);

        }
        //set scores
        for (int i = 0; i < docs.length; i++) {
            double score = (!popular.equals(this.EMPTY_VALUE) && nics[i].equals(popular)) ? 100 : 0;
            AttributeScore as = new AttributeScore(nics[i], score);

            attrScore.add(as);
        }
        return attrScore;
    }

    private ArrayList<AttributeScore> getNICCorrelationScore(Document[] docs) {
        int docsLength = docs.length;
        ArrayList<AttributeScore> attrScore = new ArrayList<>();
        String[] nics = new String[docsLength];
        //count candidates with occurrences count
        Map<String, Integer> candidates = new HashMap<>();
        for (int i = 0; i < docs.length; i++) {
            Document doc = docs[i];
            String nic = "";
            //iterate over "nic" attributes
            for (String attributeName : Constant.Attribute.NIC.getRight()) {
                if (doc.getContent().get(attributeName) != null) {
                    nic = doc.getContent().get(attributeName).toLowerCase();
                    Integer count = candidates.get(nic);
                    candidates.put(nic, count != null ? count + 1 : 1);

                    break;
                }
            }
            if (nic.isEmpty()) {
                nic = this.EMPTY_VALUE;
                Integer count = candidates.get(nic);
                candidates.put("", count != null ? count + 1 : 1);
            }

            nics[i] = nic;
        }

        //select most popular nic
        String popular = "";
        if (!candidates.isEmpty()) {
            popular = getPopularString(candidates);
        }
        //set scores
        for (int i = 0; i < docs.length; i++) {
            double score = (!popular.equals(this.EMPTY_VALUE) && nics[i].equals(popular)) ? 100 : 0;
            AttributeScore as = new AttributeScore(nics[i], score);
            attrScore.add(as);
        }

        return attrScore;
    }

    private ArrayList<AttributeScore> getGenderCorrelationScore(Document[] docs) {

        int docsLength = docs.length;
        int[] genders = new int[docsLength];
        String[] genderNames = new String[docsLength];
        ArrayList<AttributeScore> attrScore = new ArrayList<>();
        //count candidates with occurrences count
        Map<Integer, Integer> candidates = new HashMap<>();
        for (int i = 0; i < docs.length; i++) {
            Document doc = docs[i];
            String gender = this.EMPTY_VALUE;
            //iterate over "gender" attributes
            boolean isGenderSet = false;
            for (String attributeName : Constant.Attribute.SEX.getRight()) {
                if (doc.getContent().get(attributeName) != null) {
                    gender = doc.getContent().get(attributeName).toLowerCase();

                    //determine the target class of the value
                    int targetClass = getGenderClass(gender);
                    genders[i] = targetClass;

                    Integer count = candidates.get(targetClass);
                    candidates.put(targetClass, count != null ? count + 1 : 1);
                    isGenderSet = true;
                    break;
//                } else {

                }
            }
            if (!isGenderSet) {
                //there is no gender value for doc[i]
                genders[i] = 0;
                Integer count = candidates.get(0);
                candidates.put(0, count != null ? count + 1 : 1);
            }
            System.out.println("Gender: " + gender);
            genderNames[i] = gender;
        }

        if (!candidates.isEmpty()) {
            //select most popular gender
            int popular = getPopularInt(candidates);

            //set scores
            for (int i = 0; i < docs.length; i++) {
                double score = (popular != 0 && genders[i] == popular) ? 100 : 0;
                AttributeScore as = new AttributeScore(genderNames[i], score);
                attrScore.add(as);
            }
        }

        return attrScore;
    }

    private String getPopularString(Map<String, Integer> candidates) {
        if (candidates.get(this.EMPTY_VALUE) != null && candidates.get(this.EMPTY_VALUE) >= candidates.size() / 2) {
            return this.EMPTY_VALUE;
        }

        //check if all values are equal
        Set<Integer> values = new HashSet<>(candidates.values());
        if (candidates.values().size() > 1 && values.size() == 1) {
            return this.EMPTY_VALUE;
        }

        String popular = Collections.max(candidates.entrySet(),
                new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                        return o1.getValue().compareTo(o2.getValue());
                    }
                }).getKey();

        return popular;
    }

    private int getPopularInt(Map<Integer, Integer> candidates) {
        if (candidates.get(0) != null && candidates.get(0) >= candidates.size() / 2) {
            //if half or more entries are empty, the popular is the empty
            return 0;

        }

        //check if all values are equal
        Set<Integer> values = new HashSet<>(candidates.values());
        if (candidates.values().size() > 1 && values.size() == 1) {
            return 0;
        }

        int popular = Collections.max(candidates.entrySet(),
                new Comparator<Map.Entry<Integer, Integer>>() {
                    @Override
                    public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                        return o1.getValue().compareTo(o2.getValue());
                    }
                }).getKey();
        return popular;
    }

    /**
     * @param gender Eg. "male", "M" etc.
     * @return 1 if male, 2 if female
     */
    public static int getGenderClass(String gender) {
        int targetGender = 0;
        for (int j = 0; j < Constant.Attribute.Gender.TARGET_CLASSES.length; j++) {
            String[] targetClassValues = Constant.Attribute.Gender.TARGET_CLASSES[j];
            for (String s : targetClassValues) {
                if (gender.equals(s)) {
                    targetGender = j + 1;
                    break;
                }
            }
        }
        return targetGender;
    }

}
