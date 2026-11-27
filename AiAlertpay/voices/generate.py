"""Generate offline receipt audio. Build prerequisites: Python 3 and espeak-ng."""
import json
import pathlib
import subprocess
import sys
import tempfile
import wave

catalog = json.loads(pathlib.Path(__file__).with_name("catalog.json").read_text())
if len({row["tag"] for row in catalog}) != 26 or len(catalog) != 26:
    raise ValueError("Expected 26 unique payment languages")
root = pathlib.Path(sys.argv[1]) / "voices"
root.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory() as temporary:
    wav = pathlib.Path(temporary) / "speech.wav"
    for row in catalog:
        folder = root / row["tag"]
        folder.mkdir(exist_ok=True)
        clips = dict(enumerate(row.get("digits", map(str, range(10)))))
        clips.update({key: row[key] for key in ("sample", "rupee", "paise", "received")})
        for key, words in clips.items():
            subprocess.run(["espeak-ng", "-v", row["engine"], "-s", "148",
                            "-w", str(wav), words], check=True)
            with wave.open(str(wav), "rb") as audio:
                if (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) != (1, 2, 22050):
                    raise ValueError("Unexpected PCM format for " + row["tag"])
                data = audio.readframes(audio.getnframes())
                if len(data) < 1024 or not any(data):
                    raise ValueError("Empty audio for " + row["tag"] + "/" + str(key))
            (folder / (str(key) + ".pcm")).write_bytes(data)
