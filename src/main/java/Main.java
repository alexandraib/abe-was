import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Main {
	public static void main(String[] args) {
		int n = 20;
		int t = 15;
		Attribute a1 = new Attribute("team leader");
		Attribute a2 = new Attribute("SW development");
		Attribute a3 = new Attribute("available");
		Attribute[] abstractAttributeUniverse = new Attribute[]{a1, a2, a3};
		Attribute[] abstractUserAttributes = new Attribute[]{a1, a2};

		Map<Attribute, Integer> weights = new LinkedHashMap<>();
		weights.put(a1, 10);
		weights.put(a2, 6);
		weights.put(a3, 4);

		Function<Attribute, List<Attribute>> function = a -> {
			List<Attribute> ret = new ArrayList<>();
			for (int i = 0; i < weights.get(a); i++) {
				ret.add(new Attribute(a.getValue() + Integer.toString(i)));
			}
			return ret;
		};
		List<Attribute> attributeUniverse = new ArrayList<>();
		for (Attribute attribute : abstractAttributeUniverse)
			attributeUniverse.addAll(function.apply(attribute));

		List<Attribute> userAttributes = new ArrayList<>();
		for (Attribute attribute : abstractUserAttributes)
			userAttributes.addAll(function.apply(attribute));

		ABECrypt ABECrypt = new ABECrypt(n, attributeUniverse);

		String message = "important project info";
		Plaintext plaintext = new Plaintext(message);
		System.out.println("The encrypted message is: " + plaintext);

		Ciphertext ciphertext = ABECrypt.Encrypt(plaintext, userAttributes);
		List<List<Attribute>> compartments = new ArrayList<>();
		compartments.add(attributeUniverse);
		List<Integer> compartmentThresholds = new ArrayList<>();
		compartmentThresholds.add(t);

		AccessStructure cas = new AccessStructure(n, t, compartmentThresholds, compartments);

		DecryptionKey decryptionKey = ABECrypt.GenerateKey(cas);

		Plaintext decryptedMsg = new Plaintext(ABECrypt.Decrypt(cas, decryptionKey, ciphertext));

		if (decryptedMsg.getValue() != null)
			System.out.println("The decrypted message is: " + decryptedMsg);
		else
			System.out.println("The message could not be decrypted\n");
	}
}

