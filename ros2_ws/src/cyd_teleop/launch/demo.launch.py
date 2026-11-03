"""Lanza turtlesim y el driver de la pantalla táctil del CYD."""

from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node


def generate_launch_description():
    port = LaunchConfiguration('port')

    return LaunchDescription([
        DeclareLaunchArgument(
            'port', default_value='/dev/esp32_tactil',
            description='Puerto serial del CYD (p. ej. /dev/ttyUSB0)'),

        Node(package='turtlesim', executable='turtlesim_node', name='turtlesim'),

        Node(
            package='cyd_teleop', executable='driver_tactil', name='driver_tactil',
            output='screen',
            parameters=[{'port': port}],
        ),
    ])
