import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;

public class TestMain {

    public static void main(String[] args) throws UnsupportedEncodingException {
        String str = "aRR924_BFhoV-qoVp9VGS79hgGSfQCOmwUx924_BFhoV-qoV0CNMS79hEqNm2LkBXwxmS79hXq2tUUS8KuV8FrU0gXtxcR2BprNZXg9hHhV9oqoMZu3qk4_m-qxu28Y01_Yqk4_BWwx92JxmZGp924_BJwx9kCSMsBF0g7HBF9SX27Yq0GF0o6Ly6_Sx20tyB8FqgrXyo0Fxb0xByBOq24XybRFm2LfB8CpGU9eBDqppkLn8zG3ZchL1iDpuJvS8EqYhggHMcvOOSTYhKHF0oQr0oX2pr8Yqm_e0giCmU9VOgcO1KBF0g6HM-GVO2wxMEqYhgDCKevgVEwNMqBF0giU0gutpr0YBmXNqgq982TNXNqtqfXY8SQr8o8SYFq28EqYhHqeVebSYDrS8: ";
        byte[] buffer = Base64.decodeBase64(str);
        System.out.println(new String(buffer, "utf-8"));
    }

}
