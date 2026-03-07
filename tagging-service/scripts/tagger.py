#!/usr/bin/env python3

import json
import os
import sys
from pathlib import Path

import acoustid
import musicbrainzngs
import mutagen
from mutagen.easyid3 import EasyID3
from mutagen.flac import FLAC
from mutagen.mp4 import MP4

musicbrainzngs.set_useragent("naviseerr-tagger", "0.1.0", "https://github.com/naviseerr")

ACOUSTID_API_KEY = os.environ.get("ACOUSTID_API_KEY", "")
SUPPORTED_EXTENSIONS = {".mp3", ".flac", ".ogg", ".opus", ".m4a", ".wma", ".wav", ".aac"}


def find_audio_files(path: Path) -> list[Path]:
    if path.is_file():
        return [path] if path.suffix.lower() in SUPPORTED_EXTENSIONS else []
    return sorted(f for f in path.rglob("*") if f.suffix.lower() in SUPPORTED_EXTENSIONS)


def fingerprint_file(filepath: Path) -> dict:
    try:
        results = acoustid.match(ACOUSTID_API_KEY, str(filepath))
        for score, recording_id, title, artist in results:
            return {
                "file": str(filepath),
                "score": score,
                "recording_id": recording_id,
                "title": title,
                "artist": artist,
            }
        return {"file": str(filepath), "error": "no match found"}
    except acoustid.FingerprintGenerationError as e:
        return {"file": str(filepath), "error": f"fingerprint generation failed: {e}"}
    except acoustid.WebServiceError as e:
        return {"file": str(filepath), "error": f"acoustid lookup failed: {e}"}


def lookup_recording(recording_id: str) -> dict | None:
    try:
        result = musicbrainzngs.get_recording_by_id(
            recording_id, includes=["artists", "releases"]
        )
        rec = result["recording"]
        metadata = {
            "title": rec.get("title", ""),
            "artist": "",
            "album": "",
            "date": "",
            "tracknumber": "",
        }
        if "artist-credit" in rec:
            metadata["artist"] = "".join(
                credit.get("name", "") + credit.get("joinphrase", "")
                for credit in rec["artist-credit"]
                if isinstance(credit, dict)
            )
        if "release-list" in rec and rec["release-list"]:
            release = rec["release-list"][0]
            metadata["album"] = release.get("title", "")
            metadata["date"] = release.get("date", "")
        return metadata
    except musicbrainzngs.WebServiceError:
        return None


def write_tags(filepath: Path, metadata: dict) -> bool:
    ext = filepath.suffix.lower()
    try:
        if ext == ".mp3":
            try:
                audio = EasyID3(str(filepath))
            except mutagen.id3.ID3NoHeaderError:
                audio = mutagen.File(str(filepath), easy=True)
                if audio is None:
                    return False
                audio.add_tags()
            audio["title"] = metadata.get("title", "")
            audio["artist"] = metadata.get("artist", "")
            audio["album"] = metadata.get("album", "")
            audio["date"] = metadata.get("date", "")
            if metadata.get("tracknumber"):
                audio["tracknumber"] = metadata["tracknumber"]
            audio.save()
        elif ext == ".flac":
            audio = FLAC(str(filepath))
            audio["title"] = metadata.get("title", "")
            audio["artist"] = metadata.get("artist", "")
            audio["album"] = metadata.get("album", "")
            audio["date"] = metadata.get("date", "")
            if metadata.get("tracknumber"):
                audio["tracknumber"] = metadata["tracknumber"]
            audio.save()
        elif ext == ".m4a":
            audio = MP4(str(filepath))
            audio["\xa9nam"] = [metadata.get("title", "")]
            audio["\xa9ART"] = [metadata.get("artist", "")]
            audio["\xa9alb"] = [metadata.get("album", "")]
            audio["\xa9day"] = [metadata.get("date", "")]
            if metadata.get("tracknumber"):
                audio["trkn"] = [(int(metadata["tracknumber"]), 0)]
            audio.save()
        else:
            audio = mutagen.File(str(filepath), easy=True)
            if audio is None:
                return False
            audio["title"] = metadata.get("title", "")
            audio["artist"] = metadata.get("artist", "")
            audio["album"] = metadata.get("album", "")
            audio["date"] = metadata.get("date", "")
            audio.save()
        return True
    except Exception:
        return False


def cmd_fingerprint(path: Path) -> list[dict]:
    files = find_audio_files(path)
    if not files:
        return [{"error": f"no audio files found in {path}"}]
    return [fingerprint_file(f) for f in files]


def cmd_tag(path: Path) -> list[dict]:
    files = find_audio_files(path)
    if not files:
        return [{"error": f"no audio files found in {path}"}]

    results = []
    for f in files:
        fp = fingerprint_file(f)
        if "error" in fp:
            results.append({**fp, "tagged": False})
            continue

        metadata = lookup_recording(fp["recording_id"])
        if metadata is None:
            results.append({**fp, "tagged": False, "error": "musicbrainz lookup failed"})
            continue

        tagged = write_tags(f, metadata)
        results.append({**fp, "metadata": metadata, "tagged": tagged})

    return results


def main():
    if len(sys.argv) < 3:
        print(json.dumps({"error": "usage: tagger.py <fingerprint|tag> <path>"}))
        sys.exit(1)

    command = sys.argv[1]
    path = Path(sys.argv[2])

    if not path.exists():
        print(json.dumps({"error": f"path does not exist: {path}"}))
        sys.exit(1)

    if command == "fingerprint":
        results = cmd_fingerprint(path)
    elif command == "tag":
        results = cmd_tag(path)
    else:
        print(json.dumps({"error": f"unknown command: {command}"}))
        sys.exit(1)

    print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
