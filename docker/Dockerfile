# ROS 2 Lyrical con turtlesim, para probar el nodo en una máquina sin ROS nativo.
# La charla corre con ROS nativo en Kubuntu 26.04; esto no hace falta allí.
#
# Construir, desde la raíz del repo:
#   docker build -t cyd-ros-test docker/
#
# La imagen no trae el código: se monta el paquete y se compila dentro del contenedor.
#   docker run --rm -it --name demo --net=host -e QT_QPA_PLATFORM=offscreen \
#     -v "$PWD/ros2_ws/src:/src:ro" cyd-ros-test bash -c 'source /opt/ros/lyrical/setup.bash; \
#     mkdir -p /ws/src; cp -r /src/cyd_teleop /ws/src; cd /ws; colcon build; \
#     source install/setup.bash; ros2 launch cyd_teleop demo.launch.py transport:=udp'
#
# QT_QPA_PLATFORM=offscreen: turtlesim corre sin ventana; la posición se ve en /turtle1/pose.
# Para Bluetooth, cambia --net=host por --device /dev/rfcomm0 y usa port:=/dev/rfcomm0.

FROM ros:lyrical

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ros-lyrical-turtlesim \
        python3-serial \
        python3-pytest \
        netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*
