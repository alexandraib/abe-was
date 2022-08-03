import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@Setter
public class ABECrypt {
	private Pairing pairing;
	private SecretParameters masterKey;
	public PublicParameters publicKey;
	public Map<Attribute, Integer> attributeNumber;

	public ABECrypt(int n, List<Attribute> attributes) {
		PairingFactory.getInstance().setUsePBCWhenPossible(true);

		pairing = PairingFactory.getPairing("ssbm.properties");

		// set up private parameters
		Element[] t = generatePolynomial(n);
		Element y = pairing.getZr().newRandomElement();

		masterKey = new SecretParameters(y, t, n);

		// set up public parameters
		Element g = pairing.getG1().newRandomElement();
		Element egg = pairing.pairing(g, g);
		Element Y = egg.powZn(y);

		Element[] publicKeyT = new Element[n];
		for (int i = 0; i < n; i++) publicKeyT[i] = g.getField().newElement(g).powZn(masterKey.getT()[i]);

		publicKey = new PublicParameters(pairing, g, Y, publicKeyT, n);

		attributeNumber = new LinkedHashMap<>();
		for (int i = 0; i < attributes.size(); i++) {
			attributeNumber.put(attributes.get(i), i);
		}
	}

	public Ciphertext Encrypt(Plaintext plaintext, List<Attribute> attributes) {
		return encrypt(plaintext.getValue(), attributes.stream().map(a -> attributeNumber.get(a)).collect(Collectors.toList()));
	}

	private Ciphertext encrypt(BigInteger message, List<Integer> attributes) {
		Element message_GT = pairing.getGT().newElement(message);
		Element s = pairing.getZr().newRandomElement();
		Element Ys = publicKey.Y.getField().newElement(publicKey.Y).powZn(s);
		Element Ep = message_GT.getField().newElement(message_GT).mul(Ys); // M=g*Y^s
		Element S = publicKey.g.getField().newElement(publicKey.g).powZn(s); // g^s

		Element ss = pairing.getZr().newElement(s);
		Element[] E = new Element[attributes.size()];
		for (int i = 0; i < attributes.size(); i++)  // generate A and T_i
			E[i] = pairing.getG1().newElement(publicKey.T[attributes.get(i)].powZn(s));

		return new Ciphertext(Ep, S, E, attributeNumber.keySet().toArray(new Attribute[0]));
	}

	private Element PolynomialEval(Field f, List<Element> a, int degree, Element x) {
		Element tmp = f.newElement();
		Element x_pow = f.newOneElement();
		Element ans = f.newZeroElement();

		// the polynomial has degree + 1 indices
		for (int i = 0; i <= degree; i++) {
			tmp.set(a.get(i));
			tmp = tmp.mul(x_pow);
			ans = ans.add(tmp);
			x_pow = x_pow.mul(x);
		}
		return ans;
	}

	private Element PolynomialInterpolationIn0(List<Element> points, List<Element> values, int degree) {
		Element result = pairing.getGT().newOneElement();
		for (int i = 0; i < degree; i++) {
			Element exponent = pairing.getZr().newOneElement();
			for (int j = 0; j < degree; j++) {
				if (i == j) continue;
				Element tmp = pairing.getZr().newElement(points.get(i));
				tmp = tmp.sub(points.get(j));
				tmp = pairing.getZr().newZeroElement().sub(points.get(j)).div(tmp);
				exponent = exponent.mul(tmp);
			}
			Element element = publicKey.pairing.getGT().newElement(values.get(i));
			element = element.powZn(exponent);
			result = result.mul(element);
		}
		return result;
	}

	private List<Element> Share(AccessStructure cas, Element yToShare) {
		int threshold = cas.getT();
		int numberOfAttributes = cas.getN();
		List<Element> share = new ArrayList<>();


		// generate polynomial of degree (threshold - 1)
		List<Element> a = Arrays.asList(generatePolynomial(threshold));
		a.set(0, yToShare);

		for (int j = 0; j < numberOfAttributes; j++) {
			Element x = pairing.getZr().newElement(j + 1);
			Element polynomeValue = PolynomialEval(pairing.getZr(), a, threshold - 1, x);
			share.add(polynomeValue);
		}
		Element[] y = share.toArray(new Element[0]);
		return share;
	}

	private Element[] generatePolynomial(int t) {
		Element[] polynomialCoefficients = new Element[t];
		for (int i = 0; i < t; i++)
			polynomialCoefficients[i] = pairing.getZr().newRandomElement();
		return polynomialCoefficients;
	}

	public DecryptionKey GenerateKey(AccessStructure cas) {
		if (publicKey.n != cas.getN())
			return null;

		List<Element> a1 = new ArrayList<>();
		Element yToShare = pairing.getZr().newElement(masterKey.getY());
		List<Element> shares = Share(cas, yToShare);

		int numberOfAttributes = cas.getN();
		for (int i = 0; i < numberOfAttributes; i++) {
			int compartment = attributeNumber.get(cas.getCompartments().get(0).get(i));
			Element polynomeValue = shares.get(i).getField().newElement(shares.get(i));
			polynomeValue = polynomeValue.div(masterKey.t[compartment]);
			a1.add(publicKey.g.getField().newElement(publicKey.g).powZn(polynomeValue));
		}
		return new DecryptionKey(publicKey.n, a1);
	}

	public BigInteger Decrypt(AccessStructure cas, DecryptionKey decryption_key, Ciphertext ciphertext) {
		if (ciphertext.getE().length < cas.getT())
			return null;

		List<Element> share = new ArrayList<>();
		List<Element> values;
		List<Element> points;
		Attribute[] attributes = ciphertext.getAttributes();
		for (int compartmentIndex = 0; compartmentIndex < cas.getCompartments().size(); compartmentIndex++) {
			int numberOfCompartmentAttributesFound = 0, attributeCounter = 0;
			int compartmentThreshold = cas.getCompartmentThreshold().get(compartmentIndex);
			List<Attribute> compartment = cas.getCompartments().get(compartmentIndex);
			values = new ArrayList<>();//F_A
			points = new ArrayList<>();

			while (numberOfCompartmentAttributesFound < compartmentThreshold && attributeCounter < compartment.size()) {
				Attribute attribute = compartment.get(attributeCounter);
				int attributeNr = attributeNumber.getOrDefault(attribute, -1);
				int attributeIndex = IntStream.range(0, attributes.length).filter(i -> attributes[i].equals(attribute)).findFirst().orElse(-1);
				if (attributeIndex != -1) {
					points.add(pairing.getZr().newElement(attributeNr + 1));
					Field f = pairing.getG1();
					values.add(pairing.pairing(f.newElement(ciphertext.getE()[attributeIndex]), f.newElement(decryption_key.getA().get(attributeNr))));
					numberOfCompartmentAttributesFound++;
				}
				attributeCounter++;
			}
			if (numberOfCompartmentAttributesFound < compartmentThreshold)
				return null;
			share.add(pairing.getGT().newElement(PolynomialInterpolationIn0(points, values, compartmentThreshold)));
		}

		Element Ys = pairing.getGT().newElement(1);
		for (int i = 0; i < share.size(); ++i) {
			Ys = Ys.mul(pairing.getGT().newElement(share.get(i)));
		}

		Element msg_gt = pairing.getGT().newElement(ciphertext.getEp()).div(Ys);
		BigInteger decryptedMessage = msg_gt.toBigInteger();
		return decryptedMessage;
	}

}
