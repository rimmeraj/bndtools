package org.bndtools.core.ui.resource;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledString;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.FilterParser.Op;
import aQute.bnd.osgi.resource.FilterParser.RangeExpression;
import aQute.bnd.osgi.resource.FilterParser.SimpleExpression;
import aQute.bnd.osgi.resource.FilterParser.WithRangeExpression;
import bndtools.UIConstants;

public class R5LabelFormatter {

    static FilterParser filterParser = new FilterParser();

    static Pattern EE_PATTERN = Pattern.compile("osgi.ee=([^\\)]*).*version=([^\\)]*)");

    public static String getVersionAttributeName(String ns) {
        String r;

        if (ns == null)
            r = null;
        else if (ns.equals(IdentityNamespace.IDENTITY_NAMESPACE))
            r = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ContentNamespace.CONTENT_NAMESPACE))
            r = null;
        else if (ns.equals(BundleNamespace.BUNDLE_NAMESPACE))
            r = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
        else if (ns.equals(HostNamespace.HOST_NAMESPACE))
            r = HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
        else if (ns.equals(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE))
            r = ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(PackageNamespace.PACKAGE_NAMESPACE))
            r = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ExtenderNamespace.EXTENDER_NAMESPACE))
            r = ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ContractNamespace.CONTRACT_NAMESPACE))
            r = ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ServiceNamespace.SERVICE_NAMESPACE))
            r = null;
        else
            r = null;

        return r;
    }

    public static String getNamespaceImagePath(String ns) {
        String r = "icons/bullet_green.png"; // generic green dot

        if (BundleNamespace.BUNDLE_NAMESPACE.equals(ns) || HostNamespace.HOST_NAMESPACE.equals(ns))
            r = Icons.path("bundle");
        else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(ns))
            r = "icons/java.png";
        else if (PackageNamespace.PACKAGE_NAMESPACE.equals(ns))
            r = Icons.path("package");
        else if (ServiceNamespace.SERVICE_NAMESPACE.equals(ns))
            r = Icons.path("service");
        else if (ExtenderNamespace.EXTENDER_NAMESPACE.equals(ns))
            r = "icons/wand.png";
        else if (ContractNamespace.CONTRACT_NAMESPACE.equals(ns))
            r = "icons/contract.png";
        else if ("osgi.whiteboard".equals(ns))
            r = "icons/whiteboard.png";
        else if (ns.startsWith("osgi.enroute"))
            r = "enroute/enroute-color-16x16.png";
        else if ("osgi.missing".equalsIgnoreCase(ns) || "donotresolve".equalsIgnoreCase(ns) || "compile-only".equalsIgnoreCase(ns))
            r = "icons/prohibition.png";

        return r;
    }

    public static void appendNamespaceWithValue(StyledString label, String ns, String value, boolean shorten) {
        String prefix = ns;
        if (shorten) {
            if (IdentityNamespace.IDENTITY_NAMESPACE.equals(ns))
                prefix = "id";
            else if (BundleNamespace.BUNDLE_NAMESPACE.equals(ns))
                prefix = "";
            else if (HostNamespace.HOST_NAMESPACE.equals(ns))
                prefix = "host";
            else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(ns))
                prefix = "";
            else if (PackageNamespace.PACKAGE_NAMESPACE.equals(ns))
                prefix = "";
            else if (ServiceNamespace.SERVICE_NAMESPACE.equals(ns))
                prefix = "";
            else if (ContractNamespace.CONTRACT_NAMESPACE.equals(ns))
                prefix = "";
            else if ("osgi.whiteboard".equals(ns))
                prefix = "";
        }

        if (prefix.length() > 0)
            label.append(prefix + "=", StyledString.QUALIFIER_STYLER);
        label.append(value, UIConstants.BOLD_STYLER);
    }

    public static void appendCapability(StyledString label, Capability cap, boolean shorten) {
        String ns = cap.getNamespace();

        Object nsValue = cap.getAttributes().get(ns);
        String versionAttributeName = getVersionAttributeName(ns);
        if (nsValue != null) {
            appendNamespaceWithValue(label, ns, nsValue.toString(), shorten);

            if (versionAttributeName != null) {
                Object version = cap.getAttributes().get(versionAttributeName);
                if (version != null) {
                    label.append(", " + versionAttributeName, StyledString.QUALIFIER_STYLER);
                    label.append(" " + version.toString(), UIConstants.BOLD_COUNTER_STYLER);
                }
            }
        } else {
            label.append(ns, UIConstants.BOLD_STYLER);
        }
        label.append(" ", StyledString.QUALIFIER_STYLER);

        if (!cap.getAttributes().isEmpty()) {
            boolean first = true;
            for (Entry<String,Object> entry : cap.getAttributes().entrySet()) {
                String key = entry.getKey();
                if (!key.equals(ns) && !key.equals(versionAttributeName)) {
                    if (first)
                        label.append("[", StyledString.QUALIFIER_STYLER);
                    else
                        label.append(", ", StyledString.QUALIFIER_STYLER);

                    first = false;
                    label.append(key + "=", StyledString.QUALIFIER_STYLER);
                    label.append(entry.getValue() != null ? entry.getValue().toString() : "<null>", StyledString.QUALIFIER_STYLER);
                }
            }
            if (!first)
                label.append("]", StyledString.QUALIFIER_STYLER);
        }

        if (!cap.getDirectives().isEmpty()) {
            label.append(" ");
            boolean first = true;
            for (Entry<String,String> directive : cap.getDirectives().entrySet()) {
                label.append(directive.getKey() + ":=" + directive.getValue(), StyledString.QUALIFIER_STYLER);
                if (!first)
                    label.append(", ", StyledString.QUALIFIER_STYLER);
            }
        }

    }

    public static void appendResourceLabel(StyledString label, Resource resource) {
        Capability identity = ResourceUtils.getIdentityCapability(resource);
        String name = ResourceUtils.getIdentity(identity);
        if (name == null) {
            if (resource != null) {
                name = resource.toString();
            } else {
                name = "<unknown>";
            }
        }
        label.append(name, UIConstants.BOLD_STYLER);

        Version version = ResourceUtils.getVersion(identity);
        if (version != null)
            label.append(" " + version, StyledString.COUNTER_STYLER);
    }

    public static void appendRequirementLabel(StyledString label, Requirement requirement, boolean shorten) {
        String namespace = requirement.getNamespace();
        String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);

        boolean optional = Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));

        FilterParser fp = new FilterParser();
        if (filter == null) {
            label.append(namespace + ": <no filter>", UIConstants.ERROR_STYLER);
        } else {
            try {
                Expression exp = fp.parse(filter);
                if (exp instanceof WithRangeExpression) {
                    appendNamespaceWithValue(label, namespace, ((WithRangeExpression) exp).printExcludingRange(), shorten);
                    RangeExpression range = ((WithRangeExpression) exp).getRangeExpression();
                    if (range != null)
                        label.append(" ").append(formatRangeString(range), StyledString.COUNTER_STYLER);
                } else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(namespace)) {
                    Matcher matcher = EE_PATTERN.matcher(filter);
                    if (matcher.find()) {
                        String eename = matcher.group(1);
                        String version = matcher.group(2);
                        appendNamespaceWithValue(label, namespace, eename, true);
                        label.append(" ").append(version, StyledString.COUNTER_STYLER);
                    } else {
                        appendNamespaceWithValue(label, namespace, filter, true);
                    }
                } else {
                    appendNamespaceWithValue(label, namespace, filter, true);
                }
            } catch (IOException e) {
                label.append(namespace + ": ", StyledString.QUALIFIER_STYLER);
                label.append("<parse error>", UIConstants.ERROR_STYLER);
            }
        }

        boolean first = true;
        for (Entry<String,String> directive : requirement.getDirectives().entrySet()) {
            if (Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE.equals(directive.getKey()) || Namespace.REQUIREMENT_FILTER_DIRECTIVE.equals(directive.getKey()))
                continue; // deal with the filter: and resolution: directives separately
            StringBuilder buf = new StringBuilder();
            buf.append(first ? " " : ", ");
            buf.append(directive.getKey()).append(":=").append(directive.getValue());
            label.append(buf.toString(), StyledString.QUALIFIER_STYLER);
            first = false;
        }

        if (optional) {
            label.setStyle(0, label.length(), StyledString.QUALIFIER_STYLER);
            label.append(" <optional>", UIConstants.ITALIC_STYLER);
        }
    }

    public static String formatRangeString(RangeExpression range) {
        StringBuilder sb = new StringBuilder();

        SimpleExpression low = range.getLow();
        if (low == null) {
            sb.append("[0");
        } else {
            if (low.getOp() == Op.GREATER)
                sb.append("(");
            else
                sb.append("[");
            sb.append(low.getValue());
        }

        sb.append(", ");

        SimpleExpression high = range.getHigh();
        if (high == null) {
            sb.append("\u221e]"); // INFINITY Unicode: U+221E, UTF-8: E2 88 9E
        } else {
            sb.append(high.getValue());
            if (high.getOp() == Op.LESS)
                sb.append(")");
            else
                sb.append("]");
        }
        return sb.toString();
    }

}
