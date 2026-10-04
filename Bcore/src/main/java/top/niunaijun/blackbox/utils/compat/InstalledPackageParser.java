package top.niunaijun.blackbox.utils.compat;

import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import java.io.File;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.VerifiedPackageParser;

/** Parses the requested installed app without accepting an unrelated module's manifest. */
public final class InstalledPackageParser {
    private InstalledPackageParser() { }

    public static PackageParser.Package parse(PackageInfo installed) throws Throwable {
        if (installed == null || installed.applicationInfo == null || installed.applicationInfo.sourceDir == null) {
            throw new IllegalStateException("Installed package has no base APK");
        }
        File base = new File(installed.applicationInfo.sourceDir);
        PackageParser parser = PackageParserCompat.createParser(base);
        if (parser == null) throw new IllegalStateException("Android package parser is unavailable");
        PackageParser.Package parsed = VerifiedPackageParser.parse(installed.packageName, new VerifiedPackageParser.Parser<PackageParser.Package>() {
            @Override public PackageParser.Package parse(boolean explicitManifest) throws Throwable {
                return explicitManifest ? parseBaseManifest(parser, installed) : PackageParserCompat.parsePackage(parser, base, 0);
            }
            @Override public String packageName(PackageParser.Package result) { return result.packageName; }
        });
        // These remain the original APK and signatures; never rename or fabricate a Google package.
        parsed.baseCodePath = base.getAbsolutePath();
        parsed.codePath = base.getAbsolutePath();
        PackageParserCompat.collectCertificates(parser, parsed, 0);
        return parsed;
    }

    private static PackageParser.Package parseBaseManifest(PackageParser parser, PackageInfo installed) throws Exception {
        AssetManager assets = Reflector.on(AssetManager.class).constructor().newInstance();
        XmlResourceParser manifest = null;
        try {
            String path = installed.applicationInfo.sourceDir;
            Integer baseCookie = Reflector.with(assets).method("addAssetPath", String.class).call(path);
            if (baseCookie == null || baseCookie == 0) throw new IllegalStateException("Could not load the base APK's resources");
            if (installed.applicationInfo.splitSourceDirs != null) {
                for (String split : installed.applicationInfo.splitSourceDirs) {
                    if (split == null || split.equals(path)) continue;
                    Integer cookie = Reflector.with(assets).method("addAssetPath", String.class).call(split);
                    if (cookie == null || cookie == 0) throw new IllegalStateException("Could not load split APK resources: " + new File(split).getName());
                }
            }
            // Use the base's cookie, rather than whichever manifest is last in the asset list.
            manifest = assets.openXmlResourceParser(baseCookie, "AndroidManifest.xml");
            Resources hostResources = BlackBoxCore.getContext().getResources();
            Resources resources = new Resources(assets, hostResources.getDisplayMetrics(), new Configuration(hostResources.getConfiguration()));
            String[] error = new String[1];
            PackageParser.Package result = Reflector.with(parser)
                    .method("parseBaseApk", String.class, Resources.class, XmlResourceParser.class, int.class, String[].class)
                    .call(path, resources, manifest, 0, error);
            if (result == null) throw new IllegalStateException("Base manifest parse failed: " + error[0]);
            return result;
        } finally {
            if (manifest != null) manifest.close();
            assets.close();
        }
    }
}
