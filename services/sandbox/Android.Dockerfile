# Supply a reviewed image with Flutter, the Android SDK, Java and SDK licenses.
ARG FLUTTER_BASE
FROM ${FLUTTER_BASE}
USER root
RUN command -v python3 && command -v curl && command -v flutter && command -v java && test -n "$ANDROID_HOME" && install -d -o 1000 -g 1000 /workspace /home/build && chown -R 1000:1000 "$(dirname "$(dirname "$(readlink -f "$(command -v flutter)")")")" "$ANDROID_HOME"
ENV HOME=/home/build PUB_CACHE=/home/build/.pub-cache GRADLE_USER_HOME=/home/build/.gradle
WORKDIR /workspace
USER 1000:1000
RUN flutter config --no-analytics && flutter precache --android --web && flutter create --project-name cache_seed --platforms=web,android /tmp/cache_seed && cd /tmp/cache_seed && flutter build apk --debug && flutter build web && rm -rf /tmp/cache_seed
