package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.*;
import java.util.Map;

public class MHMods_UnstableShields extends mhmods_baseSHmod {

    final float DmgRed = 0.30f;
    final float DmgInc = 0.30f;
    final float SmodDmgRed = 0.40f;
    final float SmodDmgInc = 0.20f;

    public final float StableTime = 3f,
            TimeToBrake = 9f,
            TimeToUnbrake = 6f,
            TimeToStartUnbrake = 3f,
            StableTimeSMod = 4f,
            TimeToBrakeSMod = 12f,
            TimeToUnbrakeSMod = 4f,
            TimeToStartUnbrakeSMod = 2f;

    public Color ZeroEntropyColor = new Color(153, 18, 213, 255);
    public Color FullEntropyColor = new Color(254, 30, 30, 255);

    {
        id = "mhmods_unstableshields";
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        //stats.getShieldAbsorptionMult().modifyMult(id, DmgRed);
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return Math.round(DmgRed * 100) + "%";
        if (index == 1) return Math.round(StableTime) + "";
        if (index == 2) return Math.round(DmgInc * 100) + "%";
        if (index == 3) return Math.round(TimeToBrake) + "";
        if (index == 4) return Math.round(TimeToStartUnbrake) + "";
        if (index == 5) return Math.round(TimeToUnbrake) + "";
        return null;
    }

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return Math.round(SmodDmgRed * 100) + "%";
        if (index == 1) return Math.round(SmodDmgInc * 100) + "%";
        return null;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        if ((ship.getShield() == null))
            return "Ship Has No Shield";
        return null;
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && ship.getShield() != null;
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship == null || !ship.isAlive() || ship.getShield() == null) return;
		
        float Instability = 0f;
        float ShieldStart = 0f;
        float StartUnbrake = 0f;

        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        String id = ship.getId();

        if (!ship.isAlive()) return;
        if (customCombatData.get("MHMods_Instability" + id) instanceof Float)
            Instability = (float) customCombatData.get("MHMods_Instability" + id);

        if (customCombatData.get("MHMods_ShieldStart" + id) instanceof Float)
            ShieldStart = (float) customCombatData.get("MHMods_ShieldStart" + id);

        if (customCombatData.get("MHMods_StartUnbrake" + id) instanceof Float)
            StartUnbrake = (float) customCombatData.get("MHMods_StartUnbrake" + id);

        float StableTime = this.StableTime;
        float TimeToBrake = this.TimeToBrake;
        float TimeToUnbrake = this.TimeToUnbrake;
        float TimeToStartUnbrake = this.TimeToStartUnbrake;
        //Smod part
        if (isSMod(ship)) {
            StableTime = StableTimeSMod;
            TimeToBrake = TimeToBrakeSMod;
            TimeToUnbrake = TimeToUnbrakeSMod;
            TimeToStartUnbrake = TimeToStartUnbrakeSMod;
        }

        //ship.getShield().setRingColor(new Color(255, 173, 173, 255));

        float timer = Global.getCombatEngine().getTotalElapsedTime(false);

        if (ship.getShield().isOff()) {
            ShieldStart = timer;
            if (StartUnbrake > 0) {
                StartUnbrake -= amount;
            } else {
                if (Instability > 0) {
                    Instability -= ((1 / TimeToUnbrake) * amount);
                } else {
                    Instability = 0f;
                }
            }
        }

        if (ship.getShield().isOn()) {
            float timeShieldOn = timer - ShieldStart;

            if (timeShieldOn > StableTime) {
                if (Instability < 1)
                    Instability += ((1 / TimeToBrake) * amount);
                else
                    Instability = 1f;
            }
            StartUnbrake = TimeToStartUnbrake;
        }
        customCombatData.put("MHMods_Instability" + id, Instability);
        customCombatData.put("MHMods_ShieldStart" + id, ShieldStart);
        customCombatData.put("MHMods_StartUnbrake" + id, StartUnbrake);

        int ShieldRed = limit(Math.round(ZeroEntropyColor.getRed() * (1 - Instability) + FullEntropyColor.getRed() * Instability));
        int ShieldGreen = limit(Math.round(ZeroEntropyColor.getGreen() * (1 - Instability) + FullEntropyColor.getGreen() * Instability));
        int ShieldBlue = limit(Math.round(ZeroEntropyColor.getBlue() * (1 - Instability) + FullEntropyColor.getBlue() * Instability));

        ship.getShield().setInnerColor(new Color(ShieldRed, ShieldGreen, ShieldBlue, 200));

        //Dmg Mult stuff
        float ShieldAbsorptionMult = 1 - (((DmgInc + DmgRed) * (1 - Instability)) - DmgRed);
        if (isSMod(ship)) {
            ShieldAbsorptionMult = 1 - (((SmodDmgInc + SmodDmgRed) * (1 - Instability)) - SmodDmgRed);
        }

        ship.getMutableStats().getShieldAbsorptionMult().modifyMult("MHMods_UnstableShields_break", ShieldAbsorptionMult);

        if (ship == Global.getCombatEngine().getPlayerShip()) {
            Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_UnstableShield", "graphics/icons/hullsys/fortress_shield.png", "Shield damage taken", "x" + (float) Math.round(ShieldAbsorptionMult * 100) / 100, false);
        }

    }

    public int limit(int value) {
        return Math.max(0, Math.min(value, 255));
    }

}
