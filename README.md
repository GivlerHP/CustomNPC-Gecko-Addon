
## 👋 Welcome to the CustomNPC+ Gecko Addon Repository.

----------------

<a href="https://discord.gg/pQqRTvFeJ5"> <img src="https://img.shields.io/badge/KAMKEEL_Discord-7289DA?style=for-the-badge&logo=discord&logoColor=white" width="400" height="60"> </a>
<a href="https://ko-fi.com/kamkeel"> <img src="https://img.shields.io/badge/Support_Me_|_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white" alt="Support Me"  width="400" height="60"> </a>

[![Download CustomNPC+](https://img.shields.io/badge/CustomNPC+-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://modrinth.com/mod/customnpc-plus)
[![Download MPM+](https://img.shields.io/badge/MorePlayerModels+-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/moreplayermodels-plus)
[![Download PluginMod](https://img.shields.io/badge/Plugin_Mod-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://github.com/KAMKEEL/Plugin-Mod)

----------------

### ⬇️ Downloads
- **Modrinth**: [NONE]()
- **CurseForge**: [NONE]()

### 🔹 Installation
This mod is an addon mod to **CustomNPC+**. It requires both **GeckoLib** and **CustomNPC+** to be installed alongside it.

## Editing Gecko NPCs

Open **Display → Model**, then click the current model in the model editor. Search **Entities** by display name or registry ID, or search **Gecko** by model name or resource ID. The Gecko list includes models from active resource packs, installed mods, and the current server.

The **Texture** and **Animations** switches sit next to the selection buttons on the Gecko tab. Enable either switch to apply the corresponding file, and hover over it to see the resource ID. The existing clip names remain in their slots so you can review them against the selected file. When the current head bone is absent, the editor can pick `head` or `Head` if the model has one. Use **Model options** to search the geometry's bone names and set the transition length in ticks.

The preview can rotate by dragging or with the arrow controls. Use the wheel over the model or **−** and **+** to zoom. The four-corner **Fit** icon restores the fitted zoom at the current angle.

Open **Animations** in the model editor to select a file and assign Idle, Walk, Melee Attack, Ranged Attack, and Hurt clips. **Compatible** shows resources with matching bones. **All resources** lets you inspect other files and see which bones a clip is missing. Idle and Walk previews loop. Melee Attack, Ranged Attack, and Hurt play once. Confirm the animation dialog, then click **Done** in the model editor to save the draft to the NPC.

## Dialogue gestures

In **Globals → Dialogue Graph**, edit a speech and choose **Animation…**. When you open the graph from a Gecko NPC, the picker starts with its model and file. In the global graph, **Preview model…** lets you pick any loaded Gecko geometry. Use **Play** to test the clip and **Apply** to assign it. The play button next to an assigned clip opens a compact preview. A missing or partially compatible resource is shown in the picker. In the world the NPC needs the assigned animation file. Each participant sees the gesture for their current speech. The dialogue camera can turn the head bone gently toward its local player when the clip has no keyed head rotation.

Scripted and combat animations take priority. The interrupted gesture ends, and the controller returns to its usual idle or movement animation after the higher-priority animation finishes. Each new speech can start a new gesture. Transient model and resource updates can delay the start by up to 1.5 seconds. Missing resources leave the conversation available.

Use the three corresponding builds together for local testing. Before publishing the addon, set the Forge dependency floors to the first released CustomNPC+ build with the dialogue animation bridge and the first GeckoLib build with resource revision tracking.

## Cloning / Compiling / Building
1. Run gradlew build in console and wait BUILD SUCCESSFUL
2. Import project into IntelliJ
3. Wait for the IntelliJ to build the environment
