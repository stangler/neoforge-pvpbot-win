FROM eclipse-temurin:25-jdk

# 基本ツール + GLFW/LWJGL(Minecraftクライアント描画)用X11ライブラリ + GPUアクセラレーション
RUN apt-get update && apt-get install -y --no-install-recommends \
    git curl unzip ca-certificates \
    libgl1 libglu1-mesa mesa-utils \
    libgl1-mesa-dri mesa-vulkan-drivers \
    libxext6 libxrender1 libxrandr2 libxi6 libxcursor1 libxinerama1 \
    libpulse0 \
    && rm -rf /var/lib/apt/lists/*

# 非rootユーザー(DevContainer慣習)
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID
RUN if getent group $USER_GID >/dev/null; then \
        EXISTING_GROUP=$(getent group $USER_GID | cut -d: -f1); \
        groupmod -n $USERNAME $EXISTING_GROUP; \
    else \
        groupadd --gid $USER_GID $USERNAME; \
    fi \
    && if getent passwd $USER_UID >/dev/null; then \
        EXISTING_USER=$(getent passwd $USER_UID | cut -d: -f1); \
        usermod -l $USERNAME -d /home/$USERNAME -m $EXISTING_USER; \
    else \
        useradd --uid $USER_UID --gid $USER_GID -m $USERNAME; \
    fi

# named volumeマウント先の所有権を事前設定(初回マウント時に引き継がれる)
RUN mkdir -p /home/$USERNAME/.gradle \
    && chown -R $USER_UID:$USER_GID /home/$USERNAME

USER $USERNAME
WORKDIR /workspace
