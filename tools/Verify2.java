import com.android.apksig.ApkVerifier;
import java.io.File;
import java.util.List;

public class Verify2 {
    public static void main(String[] a) throws Exception {
        ApkVerifier v = new ApkVerifier.Builder(new File(a[0])).setMinCheckedPlatformVersion(21).build();
        ApkVerifier.Result r = v.verify();
        System.out.println("verified=" + r.isVerified() + " v1=" + r.isVerifiedUsingV1Scheme() + " v2=" + r.isVerifiedUsingV2Scheme());
        for (ApkVerifier.IssueWithParams e : r.getErrors()) System.out.println("ERR " + e);
        for (ApkVerifier.IssueWithParams e : r.getWarnings()) System.out.println("WARN " + e);
    }
}

