# Submission videos

Put the two recordings here. They are required deliverables and are not stored
in git (see `.gitignore`).

| File | Requirement |
| --- | --- |
| `demo.mp4` | System demonstration, MP4, **no more than 4 minutes**. Show the most important features: create the four PIR types, search, update, print, delete, save and load. |
| `presentation.mp4` | Group presentation recording, MP4, **no more than 5 minutes**. Every member speaks for at least 1 minute, shows their student ID card and face at the start, and the current speaker stays visible. |

`package_submission.ps1` refuses to build the ZIP while either file is missing.

The local `demo.mp4` prepared on 17 November 2026 is 3 minutes 47 seconds,
1920 x 1080, 25 fps, H.264 MP4. It has English captions and no audio track.
It is explicitly labelled as an automated replay of actual CLI output, with
commands and responses captured from the unmodified Java application. The
save/load section uses two separate Java processes and verifies identical IDs
and record fields after restart. The group should watch it before submission.
Local capture and verification records are in `build/demo-video/cli-session.json`
and `build/demo-video/verification.json`. The MP4 remains local and is not in Git.
