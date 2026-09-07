# Android build environment for Player Picker.
# JDK 21 + Android cmdline-tools. The tools live OUTSIDE $ANDROID_HOME because
# $ANDROID_HOME is a named volume; keeping them in the image means a rebuilt
# image always ships fresh tools, while downloaded SDK packages persist.
FROM mcr.microsoft.com/devcontainers/java:21-bookworm

ARG CMDLINE_TOOLS_VERSION=13114758

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    CMDLINE_TOOLS=/opt/android-cmdline-tools

# unzip/curl for the SDK installer; the rest are emulator runtime deps.
RUN apt-get update && apt-get install -y --no-install-recommends \
        unzip curl file \
        libglu1-mesa libpulse0 libxcursor1 libxrandr2 libxi6 libnss3 libasound2 \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "${CMDLINE_TOOLS}" \
    && curl -fsSL -o /tmp/tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    && unzip -q /tmp/tools.zip -d /tmp/tools \
    && mv /tmp/tools/cmdline-tools "${CMDLINE_TOOLS}/latest" \
    && rm -rf /tmp/tools.zip /tmp/tools \
    && mkdir -p "${ANDROID_HOME}" \
    && chown -R vscode:vscode "${CMDLINE_TOOLS}" "${ANDROID_HOME}"

ENV PATH="${CMDLINE_TOOLS}/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"

# The base image ships a full JDK 21 at /usr/lib/jvm/msopenjdk-current. Gradle
# needs the compiler, not just a runtime: against a JRE every build dies with
# "Toolchain installation ... does not provide the required capabilities:
# [JAVA_COMPILER]", and only at build time, long after the image looks fine.
# Assert it here so a base-image change that drops the JDK fails the image
# build instead.
RUN javac -version && java -version

USER vscode
