package com.ghostchu.libertybansdumper;

import com.google.gson.Gson;
import net.md_5.bungee.api.plugin.Plugin;
import space.arim.libertybans.api.*;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public final class LibertyBansDumper extends Plugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        List<PunishmentBean> beans = new ArrayList<>();
        Omnibus omnibus = OmnibusProvider.getOmnibus();
        LibertyBans libertyBans = omnibus.getRegistry().getProvider(LibertyBans.class).orElseThrow();
        ReactionStage<List<Punishment>> stage = libertyBans.getSelector().selectionBuilder()
                .types(SelectionPredicate.matchingAll())
                .build().getAllSpecificPunishments();

        try {
            stage.toCompletableFuture().get().forEach(punishment -> {
                getLogger().info(punishment.toString());
                long endDate = punishment.getEndDate().getEpochSecond();
                long id = punishment.getIdentifier();
                long startDate = punishment.getStartDate().getEpochSecond();
                boolean permanent = punishment.isPermanent();
                boolean temporary = punishment.isTemporary();
                UUID operator;
                if (punishment.getOperator().getType() == Operator.OperatorType.PLAYER) {
                    operator = ((PlayerOperator) punishment.getOperator()).getUUID();
                } else {
                    operator = new UUID(0, 0);
                }
                String reason = punishment.getReason();
                String type = punishment.getType().name();
                UUID victimUUID = new UUID(0, 0);
                String address = "";
                if (punishment.getVictim().getType() == Victim.VictimType.PLAYER) {
                    victimUUID = ((PlayerVictim) punishment.getVictim()).getUUID();
                } else if (punishment.getVictim().getType() == Victim.VictimType.ADDRESS) {
                    victimUUID = new UUID(0, 0);
                    AddressVictim addressVictim = (AddressVictim) punishment.getVictim();
                    address = addressVictim.getAddress().toInetAddress().getHostAddress();
                } else if (punishment.getVictim().getType() == Victim.VictimType.COMPOSITE) {
                    CompositeVictim compositeVictim = (CompositeVictim) punishment.getVictim();
                    victimUUID = compositeVictim.getUUID();
                    address = compositeVictim.getAddress().toInetAddress().getHostAddress();
                }
                String victimName = null;
                String operatorName = null;
                try {

                    Optional<String> optionalVictim = libertyBans.getUserResolver().lookupName(victimUUID).get();
                    if (optionalVictim.isPresent()) {
                        victimName = optionalVictim.get();
                    }
                    Optional<String> optionalOperator = libertyBans.getUserResolver().lookupName(operator).get();
                    if (optionalOperator.isPresent()) {
                        operatorName = optionalOperator.get();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                beans.add(new PunishmentBean(operator, operatorName, victimUUID, victimName, address, id, startDate, endDate, punishment.isExpired(), permanent, temporary, reason, type));
            });
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        getDataFolder().mkdirs();
        File file = new File(getDataFolder(), "punishments.json");
        try {
            Files.writeString(file.toPath(), new Gson().toJson(beans));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    static class PunishmentBean {
        UUID operator;
        String operatorName;
        UUID victim;
        String victimName;
        String ipAddress;
        long identifier;
        long startDate;
        long endDate;

        boolean isExpired;
        boolean isPermanent;
        boolean isTemporary;
        String reason;
        String type;

        public PunishmentBean(UUID operator, String operatorName, UUID victim, String victimName, String ipAddress, long identifier, long startDate, long endDate, boolean isExpired, boolean isPermanent, boolean isTemporary, String reason, String type) {
            this.operator = operator;
            this.operatorName = operatorName;
            this.victim = victim;
            this.victimName = victimName;
            this.ipAddress = ipAddress;
            this.identifier = identifier;
            this.startDate = startDate;
            this.endDate = endDate;
            this.isExpired = isExpired;
            this.isPermanent = isPermanent;
            this.isTemporary = isTemporary;
            this.reason = reason;
            this.type = type;
        }


    }
}
