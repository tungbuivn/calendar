# Use your existing base image
FROM openjdk:17-jdk-slim

# Set environment variables
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Install OpenJDK 11 and required packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    curl \
    git \
    sudo \
    libncurses5 \
    libncursesw5 \
    libtinfo5 \
    libc6 \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME to OpenJDK 11
# ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
# ENV PATH=$JAVA_HOME/bin:$PATH

# Create user tungbt with password
RUN useradd -m -s /bin/bash tungbt && \
    usermod -aG sudo tungbt && \
    echo "tungbt ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers && \
    echo "tungbt:password123" | chpasswd

USER tungbt

# Download and install Android SDK
RUN sudo mkdir -p $ANDROID_HOME && sudo chown -R tungbt:tungbt $ANDROID_HOME

COPY --chown=tungbt:tungbt ./tools.zip $ANDROID_HOME/commandlinetools-linux-9477386_latest.zip

RUN ls -la $ANDROID_HOME/

RUN cd $ANDROID_HOME && unzip ./commandlinetools-linux-9477386_latest.zip && \
    rm ./commandlinetools-linux-9477386_latest.zip



RUN sudo mkdir -p /home/tungbt/.gradle/ && sudo chown -R tungbt:tungbt /home/tungbt/.gradle

COPY --chown=tungbt:tungbt ./gradle/gradle-8.9-bin.zip /home/tungbt/.gradle/gradle-8.9.zip
# Set up Android SDK
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# RUN cd /home/tungbt/.gradle/wrapper/dists/ && unzip ./gradle-8.9.zip 

# RUN ls -la /home/tungbt/.gradle/wrapper/dists

# Accept licenses and install required SDK components
# RUN yes | sdkmanager --licenses && \
#     sdkmanager "platform-tools" \
#     "platforms;android-33" \
#     "build-tools;33.0.0" \
#     "build-tools;30.0.3" \
#     "extras;android;m2repository" \
#     "extras;google;m2repository"

# Change ownership of Android SDK to tungbt
# RUN sudo chown -R tungbt:tungbt $ANDROID_HOME

# Create app directory and set ownership
RUN sudo mkdir -p /app && sudo chown -R tungbt:tungbt /app 

# Set working directory
WORKDIR /app

# Copy project files
COPY --chown=tungbt:tungbt . /app

# Make gradlew executable
# RUN chmod +x ./gradlew

# Switch to tungbt user
USER tungbt

# Set up Gradle home for the user
ENV GRADLE_USER_HOME=/home/tungbt/.gradle

RUN mkdir -p /home/tungbt/.android/keystores

RUN keytool -genkey -v \
  -keystore /home/tungbt/.android/keystores/debug-key.jks \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"

# Create gradle native directory and trigger native download
# RUN ./gradlew --version --no-daemon

# Build the project (debug only, skip lint)
# RUN ./gradlew assembleDebug --no-daemon

# Optional: Create a volume for the build output
# VOLUME /app/app/build/outputs

# Default command
CMD ["./gradlew", "assembleDebug", "--no-daemon"] 