package net.dandare21.fracturedutils.orchestrator.action;

import com.google.gson.*;
import java.lang.reflect.Type;

public class ActionAdapter implements JsonSerializer<OrchestratorAction>, JsonDeserializer<OrchestratorAction> {
    private static final Gson RAW_GSON = new GsonBuilder().create();

    public static GsonBuilder registerAll(GsonBuilder builder) {
        ActionAdapter adapter = new ActionAdapter();
        builder.registerTypeAdapter(OrchestratorAction.class, adapter);
        builder.registerTypeAdapter(CommandAction.class, adapter);
        builder.registerTypeAdapter(WaitUntilAction.class, adapter);
        builder.registerTypeAdapter(DelayAction.class, adapter);
        builder.registerTypeAdapter(AwaitTriggerAction.class, adapter);
        builder.registerTypeAdapter(ForkSequenceAction.class, adapter);
        builder.registerTypeAdapter(RunSequenceAction.class, adapter);
        builder.registerTypeAdapter(StallParentAction.class, adapter);
        builder.registerTypeAdapter(ResumeParentAction.class, adapter);
        return builder;
    }

    @Override
    public JsonElement serialize(OrchestratorAction src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement elem = RAW_GSON.toJsonTree(src);
        if (elem.isJsonObject()) {
            elem.getAsJsonObject().addProperty("type", src.getType());
        }
        return elem;
    }

    @Override
    public OrchestratorAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonParseException("Action element must be a JSON object");
        }
        JsonObject obj = json.getAsJsonObject();
        String type = obj.has("type") ? obj.get("type").getAsString() : "";
        switch (type) {
            case "command":
                return RAW_GSON.fromJson(obj, CommandAction.class);
            case "wait_until":
                return RAW_GSON.fromJson(obj, WaitUntilAction.class);
            case "delay":
                int ticks = obj.has("ticks") ? obj.get("ticks").getAsInt() : 20;
                return new WaitUntilAction("delay", ticks, "", "");
            case "await_trigger":
                String trig = obj.has("triggerId") ? obj.get("triggerId").getAsString() : "trigger_1";
                return new WaitUntilAction("trigger", 0, trig, "");
            case "fork_sequence":
                return RAW_GSON.fromJson(obj, ForkSequenceAction.class);
            case "run_sequence":
                return RAW_GSON.fromJson(obj, RunSequenceAction.class);
            case "stall_parent":
                return RAW_GSON.fromJson(obj, StallParentAction.class);
            case "resume_parent":
                return RAW_GSON.fromJson(obj, ResumeParentAction.class);
            default:
                throw new JsonParseException("Unknown action type: '" + type + "' in JSON object: " + json);
        }
    }
}
