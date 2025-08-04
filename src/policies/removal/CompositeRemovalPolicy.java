package com.jedk1.jedcore.policies.removal;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CompositeRemovalPolicy implements RemovalPolicy {
    private final List<RemovalPolicy> policies = new ArrayList<>();
    private final CoreAbility ability;

    public CompositeRemovalPolicy(CoreAbility ability, List<RemovalPolicy> policies) {
        if (policies != null) {
            this.policies.addAll(policies);
        }
        this.ability = ability;
    }

    public CompositeRemovalPolicy(CoreAbility ability, RemovalPolicy... policies) {
        if (policies != null) {
            this.policies.addAll(Arrays.asList(policies));
        }
        this.ability = ability;
    }

    @Override
    public boolean shouldRemove() {
        if (policies.isEmpty()) return false;

        for (RemovalPolicy policy : policies) {
            if (policy.shouldRemove()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads each policy’s configuration.
     * Policies with no config or disabled ones are removed.
     */
    public void load(ConfigurationSection config, String prefix) {
        if (policies.isEmpty() || config == null) return;

        String pathPrefix = prefix + ".RemovalPolicy.";

        Iterator<RemovalPolicy> iterator = policies.iterator();
        while (iterator.hasNext()) {
            RemovalPolicy policy = iterator.next();
            ConfigurationSection section = config.getConfigurationSection(pathPrefix + policy.getName());

            if (section == null) {
                iterator.remove();
                continue;
            }

            if (!section.getBoolean("Enabled", true)) {
                iterator.remove();
                continue;
            }

            policy.load(section);
        }
    }

    /**
     * Loads policy config using a convention: "Abilities.[Element].[AbilityName]"
     */
    @Override
    public void load(ConfigurationSection config) {
        if (config == null || this.policies.isEmpty()) return;

        Element element = ability.getElement();
        if (element instanceof Element.SubElement) {
            element = ((Element.SubElement) element).getParentElement();
        }

        String abilityName = ability.getName();
        String prefix = "Abilities." + element.getName() + "." + abilityName;

        load(config, prefix);
    }

    public void addPolicy(RemovalPolicy policy) {
        if (policy != null) {
            this.policies.add(policy);
        }
    }

    public void removePolicyType(Class<? extends RemovalPolicy> type) {
        policies.removeIf(policy -> type.isAssignableFrom(policy.getClass()));
    }

    @Override
    public String getName() {
        return "Composite";
    }
}
